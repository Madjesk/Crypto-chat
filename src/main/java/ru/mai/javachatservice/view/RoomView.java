package ru.mai.javachatservice.view;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
//import ru.mai.javachatservice.cipher.Cipher;
import ru.mai.javachatservice.cipher.SymmetricEncryption;
import ru.mai.javachatservice.kafka.KafkaWriter;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;
import ru.mai.javachatservice.model.messages.KeyMessage;
import ru.mai.javachatservice.model.messages.Message;
import ru.mai.javachatservice.model.messages.json_parser.CipherInfoMessageParser;
import ru.mai.javachatservice.model.messages.json_parser.MessageParser;
import ru.mai.javachatservice.server.ChatServer;

//import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Route("room")
public class RoomView extends VerticalLayout implements HasUrlParameter<String> {
    private final ChatServer server;
    private long clientId;
    private long roomId;
    private final KafkaWriter kafkaWriter;
    private String outputTopic;
    private volatile SymmetricEncryption symmetricEncryptionEncrypt;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private MessagesLayoutWrapper messagesLayoutWrapper;
    private long anotherClientId;
    private final Backend backend;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {

        String[] params = parameter.split("/");

        clientId = Long.parseLong(params[0]);
        roomId = Long.parseLong(params[1]);

        if (server.notExistClient(clientId)) {
            Notification.show("Пользователь не найден");
            setEnabled(false);
        } else {
            service.submit(backend::startKafka);
            server.addWindow("room/" + clientId + "/" + roomId, event.getUI());
        }
    }

    public RoomView(ChatServer server, KafkaWriter kafkaWriter) {
        this.server = server;
        this.kafkaWriter = kafkaWriter;
        this.outputTopic = null;
        this.symmetricEncryptionEncrypt = null;
        new Frontend().setPage();
        this.backend = new Backend();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        server.disconnectFromRoom(clientId, roomId);

        if (outputTopic != null) {
            kafkaWriter.processing(new Message("disconnect", null, null, 0, null).toBytes(), outputTopic);
        }

        server.disconnectFromRoom(clientId, roomId);
        backend.close();

        service.shutdown();

        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("end service");
    }

    public class MessagesLayoutWrapper {
        private final VerticalLayout messagesLayout;
        private final KafkaWriter kafkaWriter;

        public enum Destination {
            OWN,
            ANOTHER
        }

        public MessagesLayoutWrapper(VerticalLayout messagesLayout, KafkaWriter kafkaWriter) {
            this.messagesLayout = messagesLayout;
            this.kafkaWriter = kafkaWriter;
        }

        public void showTextMessage(String textMessage, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div messageDiv = new Div();

                    String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                    Span timeSpan = new Span(timestamp);
                    timeSpan.getStyle()
                            .set("font-size", "0.8em")
                            .set("color", "#888")
                            .set("position", "absolute")
                            .set("bottom", "5px")
                            .set("right", "10px");

                    Div textDiv = new Div();
                    textDiv.setText(textMessage);

                    if (destination.equals(Destination.OWN)) {
                        messageDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff")
                                .set("position", "relative");

                        setPossibilityToDelete(messagesLayout, messageDiv);
                    } else {
                        messageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2")
                                .set("position", "relative");
                    }

                    messageDiv.getStyle()
                            .set("border-radius", "5px")
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd")
                            .set("min-height", "40px");


                    messageDiv.add(textDiv, timeSpan);

                    messagesLayout.add(messageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showImageMessage(String nameFile, byte[] data, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div imageDiv = new Div();

                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));
                    Image image = new Image(resource, "Uploaded image");

                    imageDiv.add(image);

                    if (destination.equals(Destination.OWN)) {
                        imageDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
                        setPossibilityToDelete(messagesLayout, imageDiv);
                    } else {
                        imageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    imageDiv.getStyle()
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");

                    messagesLayout.add(imageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showFileMessage(String nameFile, byte[] data, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(nameFile, event -> downloadLink.getElement().callJsFunction("click"));

                    fileDiv.add(downloadButton, downloadLink);

                    if (destination.equals(Destination.OWN)) {
                        fileDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");

                        setPossibilityToDelete(messagesLayout, fileDiv);
                    } else {
                        fileDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    fileDiv.getStyle()
                            .set("display", "inline-block")
                            .set("max-width", "80%")
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");

                    messagesLayout.add(fileDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        private void setPossibilityToDelete(VerticalLayout messagesLayout, Div fileDiv) {
            messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");

            fileDiv.addClickListener(event -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.setCloseOnEsc(true);
                confirmDialog.setCloseOnOutsideClick(true);

                VerticalLayout dialogLayout = new VerticalLayout();
                dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);

                Span message = new Span("Вы хотите удалить сообщение?");
                Button confirmButton = new Button("Да", e -> {
                    int indexMessage = messagesLayout.indexOf(fileDiv);
                    messagesLayout.remove(fileDiv);
                    kafkaWriter.processing(new Message("delete_message", "text", null, indexMessage, null).toBytes(), outputTopic);
                    confirmDialog.close();
                });
                Button cancelButton = new Button("Нет", e -> confirmDialog.close());

                HorizontalLayout buttonsLayout = new HorizontalLayout(confirmButton, cancelButton);
                dialogLayout.add(message, buttonsLayout);

                confirmDialog.add(dialogLayout);
                confirmDialog.open();
            });
        }

        private void clearMessages() {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(messagesLayout::removeAll);
            }
        }

        private void deleteMessage(int index) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(() -> {
                    Component componentToRemove = messagesLayout.getComponentAt(index);
                    messagesLayout.remove(componentToRemove);
                });
            }
        }
    }

    public class Frontend {
        private static final String TYPE_MESSAGE = "message";
        private final TextField messageField;
        private final List<Pair<String, InputStream>> filesData = new ArrayList<>();
        private VerticalLayout layoutColumn3;

        public Frontend() {
            messageField = new TextField();
            messageField.setWidth("95%");

            HorizontalLayout layoutRow = new HorizontalLayout();
            layoutColumn3 = new VerticalLayout();
            H3 h3 = new H3();
            Hr hr = new Hr();

            layoutRow.addClassName("gap-medium");
            layoutRow.setWidth("100%");
            layoutRow.getStyle().set("flex-grow", "1");

            layoutColumn3.addClassName("padding-xsmall");
            layoutColumn3.setWidth("100%");
            layoutColumn3.setHeight("100%");
            layoutColumn3.getStyle().set("flex-grow", "1");
            layoutColumn3.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            layoutColumn3.setAlignItems(FlexComponent.Alignment.START);

            add(layoutRow);
            layoutRow.add(layoutColumn3);

            layoutRow.setHeight("100%");
            setHeight("100vh");
        }

        public void setPage() {
            VerticalLayout messagesLayout = new VerticalLayout();

            messagesLayout.getStyle()
                    .set("max-width", "100%")
                    .set("max-height", "100%")
                    .set("border", "2px solid black")
                    .set("padding", "10px")
                    .set("overflow-y", "auto");

            messagesLayout.setWidth("100%");
            messagesLayout.setHeight("100%");

            HorizontalLayout inputLayout = getInputLayout();
            layoutColumn3.add(messagesLayout, inputLayout);
            layoutColumn3.setFlexGrow(1.0, messagesLayout);

            messagesLayoutWrapper = new MessagesLayoutWrapper(messagesLayout, kafkaWriter);
        }

        public void sendMessage(Upload upload) {
            if (symmetricEncryptionEncrypt == null) {
                Notification.show("Ошибка: не удалось отправить сообщение");
            } else {
                try {
                    for (Pair<String, InputStream> file : filesData) {
                        byte[] bytesFile = readBytesFromInputStream(file.getRight());
                        String format = getTypeFormat(file.getLeft());
                        Message message = new Message(TYPE_MESSAGE, format, file.getLeft(), 0, bytesFile);
                        byte[] messageBytes = message.toBytes();
                        kafkaWriter.processing(symmetricEncryptionEncrypt.encrypt(messageBytes), outputTopic);
                        server.saveMessage(clientId, anotherClientId, message);

                        if (format.equals("image")) {
                            messagesLayoutWrapper.showImageMessage(file.getLeft(), bytesFile, MessagesLayoutWrapper.Destination.OWN);
                        } else {
                            messagesLayoutWrapper.showFileMessage(file.getLeft(), bytesFile, MessagesLayoutWrapper.Destination.OWN);
                        }
                    }

                    upload.clearFileList();
                    filesData.clear();

                    String textMessage = messageField.getValue();

                    if (!textMessage.isEmpty()) {
                        Message message = new Message(TYPE_MESSAGE, "text", "text", 0, textMessage.getBytes());
                        byte[] messageBytes = message.toBytes();
                        kafkaWriter.processing(symmetricEncryptionEncrypt.encrypt(messageBytes), outputTopic);
                        server.saveMessage(clientId, anotherClientId, message);
                        messagesLayoutWrapper.showTextMessage(textMessage, MessagesLayoutWrapper.Destination.OWN);
                    }

                    messageField.clear();
                } catch (IOException | RuntimeException | ExecutionException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                    Thread.currentThread().interrupt();
                }
            }
        }

        private HorizontalLayout getInputLayout() {
            HorizontalLayout inputLayout = new HorizontalLayout();
            inputLayout.setWidth("100%");
            inputLayout.setAlignItems(Alignment.BASELINE);
            inputLayout.setJustifyContentMode(JustifyContentMode.START);
            inputLayout.setSpacing(true);

            Upload upload = getUploadButton();
            Button emojiButton = createEmojiButton();

            Button sendButtonText = new Button("Отправить");
            sendButtonText.addClickListener(e -> sendMessage(upload));

            inputLayout.add(upload, emojiButton, messageField, sendButtonText);
            inputLayout.getStyle().set("margin", "0");
            inputLayout.getStyle().set("padding", "0");
            return inputLayout;
        }

        private Button createEmojiButton() {
            Button emojiButton = new Button("😊");
            emojiButton.addClickListener(event -> openEmojiDialog());
            return emojiButton;
        }

        private void openEmojiDialog() {
            Dialog emojiDialog = new Dialog();
            VerticalLayout emojiLayout = new VerticalLayout();

            List<String> emojis = Arrays.asList("😀", "😂", "😍", "👍", "🙏", "🎉", "🔥", "❤️", "❤", "😎", "😉", "😜", "🤔");

            HorizontalLayout rowLayout = new HorizontalLayout();
            for (int i = 0; i < emojis.size(); i++) {
                if (i > 0 && i % 4 == 0) {
                    emojiLayout.add(rowLayout);
                    rowLayout = new HorizontalLayout();
                }

                Button emojiButton = new Button(emojis.get(i));
                int finalI = i;
                emojiButton.addClickListener(event -> {
                    messageField.setValue(messageField.getValue() + emojis.get(finalI));
                    emojiDialog.close();
                });
                rowLayout.add(emojiButton);
            }
            emojiLayout.add(rowLayout);

            emojiDialog.add(emojiLayout);
            emojiDialog.open();
        }




        private Upload getUploadButton() {
            MultiFileMemoryBuffer multiFileMemoryBuffer = new MultiFileMemoryBuffer();
            Upload uploadButton = new Upload(multiFileMemoryBuffer);
            Button buttonLoadFile = new Button("\uD83D\uDCCE");

            buttonLoadFile.setWidth("75px");

            uploadButton.setUploadButton(buttonLoadFile);
            uploadButton.setWidth("75px");
            uploadButton.getStyle()
                    .set("padding", "0")
                    .set("margin", "0")
                    .set("border", "none");
            uploadButton.setDropLabel(new Span(""));
            uploadButton.setDropLabelIcon(new Span(""));

            uploadButton.addSucceededListener(event -> {
                String fileName = event.getFileName();
                filesData.add(Pair.of(fileName, multiFileMemoryBuffer.getInputStream(fileName)));
            });

            return uploadButton;
        }

        private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int bytesRead;
            byte[] data = new byte[1024];

            while ((bytesRead = inputStream.read(data, 0, data.length)) > 0) {
                buffer.write(data, 0, bytesRead);
            }

            buffer.flush();

            return buffer.toByteArray();
        }


        private VerticalLayout getMessagesLayout() {
            VerticalLayout layout = new VerticalLayout();

            layout.getStyle()
                    .set("max-width", "620px")
                    .set("max-height", "500px")
                    .set("border", "1px dashed #4A90E2")
                    .set("border-radius", "5px")
                    .set("padding", "10px")
                    .set("overflow-y", "auto");

            layout.setWidth("620px");
            layout.setHeight("500px");

            return layout;
        }

        String getTypeFormat(String fileName) {
            int lastDotIndex = fileName.lastIndexOf('.');
            String extension = fileName.substring(lastDotIndex + 1);

            if (extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {
                return "image";
            }

            return "other";
        }
    }

    public class Backend {
        private static final String bootstrapServer = "localhost:9093";
        private static final String autoOffsetReset = "earliest";
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private static final Random RANDOM = new Random();
        private volatile SymmetricEncryption symmetricEncryptionDecrypt;
        private volatile boolean isRunning = true;
        private CipherInfoMessage cipherInfoAnotherClient;
        private byte[] privateKey;
        private byte[] publicKeyAnother;
        private byte[] p;

        public void startKafka() {
            CipherInfoMessage cipherInfoThisClient = server.getCipherInfoMessageClient(clientId, roomId);

            KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(
                    Map.of(
                            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer,
                            ConsumerConfig.GROUP_ID_CONFIG, "group_" + clientId + "_" + roomId,
                            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset
                    ),
                    new ByteArrayDeserializer(),
                    new ByteArrayDeserializer()
            );
            kafkaConsumer.subscribe(Collections.singletonList("input_" + clientId + "_" + roomId));

            try {
                while (isRunning) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                        String jsonMessage = new String(consumerRecord.value());

                        if (jsonMessage.contains("cipher_info")) {
                            cipherInfoAnotherClient = OBJECT_MAPPER.readValue(jsonMessage, CipherInfoMessage.class);

                            outputTopic = "input_" + cipherInfoAnotherClient.getAnotherClientId() + "_" + roomId;
                            privateKey = generatePrivateKey();
                            p = cipherInfoAnotherClient.getP();
                            anotherClientId = cipherInfoAnotherClient.getAnotherClientId();
                            byte[] publicKey = generatePublicKey(privateKey, p, cipherInfoAnotherClient.getG());

                            log.info("Client {} get cipher info", clientId);
                            log.info(cipherInfoAnotherClient.toString());

                            kafkaWriter.processing(new KeyMessage("key_info", publicKey).toBytes(), outputTopic);

                            if (publicKeyAnother != null) {
                                cipherInfoAnotherClient.setPublicKey(publicKeyAnother);
                                symmetricEncryptionDecrypt = CipherInfoMessageParser.getCipher(cipherInfoAnotherClient, new BigInteger(privateKey), new BigInteger(p));

                                cipherInfoThisClient.setPublicKey(publicKeyAnother);
                                symmetricEncryptionEncrypt = CipherInfoMessageParser.getCipher(cipherInfoThisClient, new BigInteger(privateKey), new BigInteger(p));
                            }
                        } else if (jsonMessage.contains("key_info")) {
                            log.info("Client {} get key info", clientId);

                            KeyMessage keyMessage = OBJECT_MAPPER.readValue(jsonMessage, KeyMessage.class);

                            if (cipherInfoAnotherClient != null) {
                                cipherInfoAnotherClient.setPublicKey(keyMessage.getPublicKey());
                                symmetricEncryptionDecrypt = CipherInfoMessageParser.getCipher(cipherInfoAnotherClient, new BigInteger(privateKey), new BigInteger(p));

                                cipherInfoThisClient.setPublicKey(keyMessage.getPublicKey());
                                symmetricEncryptionEncrypt = CipherInfoMessageParser.getCipher(cipherInfoThisClient, new BigInteger(privateKey), new BigInteger(p));
                            } else {
                                publicKeyAnother = keyMessage.getPublicKey();
                            }
                        } else if (jsonMessage.contains("delete_message")) {
                            log.info("get disconnect message");
                            Message deleteMessage = OBJECT_MAPPER.readValue(jsonMessage, Message.class);
                            messagesLayoutWrapper.deleteMessage(deleteMessage.getIndexMessage());
                        } else if (jsonMessage.contains("disconnect")) {
                            symmetricEncryptionDecrypt = null;
                            symmetricEncryptionEncrypt = null;
                            messagesLayoutWrapper.clearMessages();
                        } else {
                            Message message = MessageParser.parseMessage(new String(symmetricEncryptionDecrypt.decrypt(consumerRecord.value())));

                            if (message != null && message.getBytes() != null) {
                                log.info("Client {} get message", clientId);

                                server.saveMessage(anotherClientId, clientId, message);

                                if (message.getTypeFormat().equals("text")) {
                                    messagesLayoutWrapper.showTextMessage(new String(message.getBytes()), MessagesLayoutWrapper.Destination.ANOTHER);
                                } else if (message.getTypeFormat().equals("image")) {
                                    messagesLayoutWrapper.showImageMessage(message.getFileName(), message.getBytes(), MessagesLayoutWrapper.Destination.ANOTHER);
                                } else {
                                    messagesLayoutWrapper.showFileMessage(message.getFileName(), message.getBytes(), MessagesLayoutWrapper.Destination.ANOTHER);
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.error(ex.getMessage());
                log.error(Arrays.deepToString(ex.getStackTrace()));
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error(ex.getMessage());
                log.error(Arrays.deepToString(ex.getStackTrace()));
            }

            kafkaConsumer.close();

            log.info("End kafka reader client {}", clientId);
        }

        private byte[] generatePrivateKey() {
            return new BigInteger(100, RANDOM).toByteArray();
        }

        private byte[] generatePublicKey(byte[] privateKey, byte[] p, byte[] g) {
            BigInteger pNumber = new BigInteger(p);
            BigInteger gNumber = new BigInteger(g);
            BigInteger key = new BigInteger(privateKey);
            return gNumber.modPow(key, pNumber).toByteArray();
        }

        public void close() {
            isRunning = false;
        }
    }
}
