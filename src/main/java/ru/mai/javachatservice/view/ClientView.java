package ru.mai.javachatservice.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import ru.mai.javachatservice.server.ChatServer;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.flow.component.dialog.Dialog;

@Route("")
public class ClientView extends VerticalLayout implements HasUrlParameter<String> {

    private long clientId;
    private String username;
    private final ChatServer server;
    private VerticalLayout layoutRow2;
    private H5 h5;
    private VerticalLayout layoutColumn2;
    private Map<String, HorizontalLayout> chatComponentsMap = new HashMap<>();

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        this.clientId = Long.parseLong(parameter);
    }

    public ClientView(ChatServer server) {
        this.server = server;

        layoutColumn2 = new VerticalLayout();
        H2 h2 = new H2();
        Hr hr = new Hr();
        h5 = new H5();

        layoutRow2 = new VerticalLayout();
        layoutRow2.setWidthFull();

        Button showFormButton = createShowFormButton();

        layoutColumn2.setAlignSelf(FlexComponent.Alignment.CENTER, h2);
        layoutColumn2.setWidth("50%");
        layoutColumn2.setHeight("100%");
        layoutColumn2.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        layoutColumn2.getStyle().set("margin", "0 auto");

        h2.setText("Список чатов");
        h5.setText("Здесь пока пусто");
        layoutColumn2.add(showFormButton);
        layoutColumn2.add(h2);
        layoutColumn2.add(hr);
        layoutColumn2.add(h5);
        layoutColumn2.add(layoutRow2);
        this.add(layoutColumn2);
        this.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        this.setHeightFull();
    }

    private void addAvatarWithButton(String roomId) {
        if (layoutColumn2.getChildren().anyMatch(component -> component.equals(h5))) {
            layoutColumn2.remove(h5);
        }
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();

        Avatar avatar = new Avatar(roomId);
        avatar.setImage("https://sun9-42.userapi.com/impg/ASuSO6aR0vZ8htOH8yQdcOdPZwf-8flro3n5mg/Je0qpHy4KHM.jpg?size=1200x1200&quality=96&sign=3300103ecf0d67db00713caa9ab3138a&c_uniq_tag=5BDbvFKNDDH1hM72VtwOvlfy3RmGVo7Xv8vHoqBblas&type=album");

        Button onChatBtn = getChatInfoButton(roomId);
        onChatBtn.setWidth("87%");

        Button deleteBtn = new Button("❌", e -> {
            removeChatFromLayout(roomId);
            server.disconnectFromRoom(clientId, Long.parseLong(roomId));
            Notification.show("Чат " + roomId + " удален.");
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.getStyle().set("margin-left", "auto");

        row.add(avatar, onChatBtn, deleteBtn);
        layoutRow2.add(row);
        chatComponentsMap.put(roomId, row);
    }

    private Button createShowFormButton() {
        Button showFormButton = new Button("Добавить чат");
        showFormButton.setWidth("50%");
        showFormButton.getStyle().set("margin", "0 auto");

        showFormButton.addClickListener(event -> {
            TextField roomIdField = new TextField("Введите id комнаты");
            roomIdField.setWidthFull();

            Dialog addChatDialog = new Dialog();
            Button connectBtn = new Button("Подключиться", clickEvent -> {
                String roomId = roomIdField.getValue();

                if (roomId.matches("\\d+")) {
                    startChat(roomId);
                    addChatDialog.close();
                } else {
                    Notification.show("Введите корректный ID комнаты (только числа)");
                }
            });
            connectBtn.setWidthFull();

            VerticalLayout dialogLayout = new VerticalLayout(roomIdField, connectBtn);
            addChatDialog.add(dialogLayout);
            addChatDialog.open();
        });

        return showFormButton;
    }

    private void removeChatFromLayout(String roomId) {
        HorizontalLayout chatRow = chatComponentsMap.get(roomId);
        if (chatRow != null) {
            layoutRow2.remove(chatRow);
            chatComponentsMap.remove(roomId);
            if (chatComponentsMap.isEmpty()) {
                layoutColumn2.add(h5);
            }
        }
    }

    private void startChat(String roomId) {
        if (roomId.isEmpty()) {
            Notification.show("ID комнаты не может быть пустым");
            return;
        }
        String url = "room/" + clientId + "/" + roomId;
        if (server.connectToRoom(clientId, Long.parseLong(roomId))) {
            if (server.isNotOpenWindow(url)) {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url);
                addAvatarWithButton(roomId);
            } else {
                Notification.show("Ошибка: чат уже открыт");
            }
        } else {
            Notification.show("Ошибка подключения: комната уже занята");
        }
    }

    private Button getChatInfoButton(String roomId) {
        String url = "room/" + clientId + "/" + roomId;

        Button chatInfoButton = new Button("Войти в чат №" + roomId, e -> {
            if (server.isNotOpenWindow(url) && server.connectToRoom(clientId, Long.parseLong(roomId))) {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url);
            } else {
                if (!server.isNotOpenWindow(url)) {
                    Notification.show("Ошибка подключения: вы уже находитесь в комнате");
                } else {
                    Notification.show("Ошибка подключения: комната уже занята");
                }
            }
        });

        return chatInfoButton;
    }
}