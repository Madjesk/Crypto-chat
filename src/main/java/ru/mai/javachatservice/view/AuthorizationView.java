package ru.mai.javachatservice.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.authorization.Authorization;
import ru.mai.javachatservice.model.client.ClientInfo;

import java.util.Map;

@Slf4j
@Route("")
public class AuthorizationView extends VerticalLayout {
    private final Authorization authorization;
    private String username;

    public AuthorizationView(Authorization authorization) {
        this.authorization = authorization;

        TextField usernameField = new TextField("Введите ваш ник");
        Button loginButton = new Button("Вход", event -> {
            this.username  = usernameField.getValue();
            if (!username.isEmpty()) {
                if (!username.matches("^[a-zA-Z0-9]+$")) {
                    Notification.show("Невозможно войти, используйте только латинские буквы");
                } else {
                    showEncryptionOptions();
                }
            } else {
                Notification.show("Вы не указали, как вас ник");
            }
        });

        usernameField.setWidth("250px");
        loginButton.setWidth("250px");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(usernameField, loginButton);
    }

    private void showEncryptionOptions() {
        // Алгоритмы шифрования
        ComboBox<String> encryptionAlgorithmComboBox = new ComboBox<>("Выберите алгоритм шифрования");
        encryptionAlgorithmComboBox.setItems("SERPENT", "RC6");
        encryptionAlgorithmComboBox.setValue("SERPENT");
        encryptionAlgorithmComboBox.setWidthFull();

        // Информационная кнопка для алгоритмов шифрования
        Button infoAlgorithmButton = createInfoButton("Алгоритмы шифрования",
                "SERPENT – Надёжный и очень безопасный алгоритм. Он подходит для защиты данных, где требуется высокий уровень безопасности. Работает медленнее, но обеспечивает отличную защиту.\n" +
                        "RC6 – Быстрый алгоритм шифрования, подходит для случаев, где важна скорость. Однако он может уступать в безопасности в некоторых сценариях, по сравнению с SERPENT.\n" +
                        "Совет: Выберите SERPENT, если не уверены, так как он более безопасен.");

        // Режимы шифрования
        ComboBox<String> encryptionModeComboBox = new ComboBox<>("Выберите режим шифрования");
        encryptionModeComboBox.setItems("ECB", "CBC", "PCBC", "CFB", "OFB", "CTR", "RANDOM_DELTA");
        encryptionModeComboBox.setValue("ECB");
        encryptionModeComboBox.setWidthFull();

        // Информационная кнопка для режимов шифрования
        Button infoModeButton = createInfoButton("Режимы шифрования",
                "ECB (Electronic Codebook) – Самый простой режим шифрования. Однако его не рекомендуется использовать для защиты конфиденциальных данных, так как он менее безопасен.\n" +
                        "CBC (Cipher Block Chaining) – Более безопасный режим, так как каждый блок данных шифруется с использованием предыдущего блока. Рекомендуется для большинства случаев.\n" +
        "PCBC (Propagating Cipher Block Chaining) – Похож на CBC, но более сложен и редко используется.\n" +
                "CFB (Cipher Feedback) – Работает с небольшими фрагментами данных. Подходит для потоковой передачи данных.\n" +
                "OFB (Output Feedback) – Подходит для случаев, когда данные передаются как поток, но с более высокой безопасностью.\n" +
                "CTR (Counter Mode) – Современный и быстрый режим шифрования, который хорошо подходит для параллельной обработки данных.\n" +
                "RANDOM_DELTA – Экспериментальный режим, использующий случайные значения. Обычно не используется для реальных задач.\n");



        // Режимы набивки
        ComboBox<String> paddingComboBox = new ComboBox<>("Выберите режим набивки");
        paddingComboBox.setItems("ZEROS", "ANSI_X923", "PKCS7", "ISO_10126");
        paddingComboBox.setValue("ZEROS");
        paddingComboBox.setWidthFull();

        // Информационная кнопка для режимов набивки
        Button infoPaddingButton = createInfoButton("Режимы набивки",
                "ZEROS – Заполняет пустое место нулями. Этот режим подходит, если данные фиксированной длины.\n" +
                        "ANSI_X923 – Добавляет нули и указывает количество добавленных байт в конце. Более безопасен, чем ZEROS\n" +
                        "PKCS7 – Самый распространённый и универсальный способ набивки. Подходит для любых данных.\n" +
                        "ISO_10126 – Похож на PKCS7, но добавляет случайные байты, кроме последнего, что может дать дополнительную безопасность.\n" +
                        "Совет: Если не знаете, какой выбрать, остановитесь на PKCS7, он является стандартным и надёжным вариантом для большинства задач.\n");

        Dialog dialog = new Dialog();

        Button submitButton = new Button("Подтвердить", event -> {
            String selectedAlgorithm = encryptionAlgorithmComboBox.getValue();
            String selectedMode = encryptionModeComboBox.getValue();
            String selectedPadding = paddingComboBox.getValue();

            ClientInfo clientInfo = authorization.authorize(username, selectedAlgorithm, selectedPadding, selectedMode);
            Notification.show("Вход выполнен успешно");
            UI.getCurrent().navigate(String.valueOf(clientInfo.getId()));

            dialog.close();
        });

        // Горизонтальные компононовки с ComboBox и информационными кнопками
        HorizontalLayout algorithmLayout = createHorizontalLayoutWithIcon(encryptionAlgorithmComboBox, infoAlgorithmButton);
        HorizontalLayout modeLayout = createHorizontalLayoutWithIcon(encryptionModeComboBox, infoModeButton);
        HorizontalLayout paddingLayout = createHorizontalLayoutWithIcon(paddingComboBox, infoPaddingButton);

        VerticalLayout dialogLayout = new VerticalLayout(algorithmLayout, modeLayout, paddingLayout, submitButton);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        dialog.add(dialogLayout);
        dialog.setWidth("400px");
        dialog.open();
    }

    // Создание информационных кнопок
    private Button createInfoButton(String title, String message) {
        Button infoButton = new Button(new Icon(VaadinIcon.INFO_CIRCLE));
        infoButton.getStyle().set("margin-top", "15px");  // Поднятие иконки на уровень ComboBox
        infoButton.addClickListener(event -> showInfoDialog(title, message));
        return infoButton;
    }

    // Создание горизонтальной компоновки с ComboBox и иконкой
    private HorizontalLayout createHorizontalLayoutWithIcon(ComboBox<String> comboBox, Button infoButton) {
        HorizontalLayout layout = new HorizontalLayout(comboBox, infoButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);  // Выравнивание иконки по центру ComboBox
        return layout;
    }

    // Диалоговое окно с информацией
    private void showInfoDialog(String title, String message) {
        Dialog infoDialog = new Dialog();
        infoDialog.setHeaderTitle(title);
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(message);
        infoDialog.add(dialogLayout);
        infoDialog.setWidth("300px");
        infoDialog.open();
    }
}