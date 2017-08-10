package com.marlontrujillo.eru.gui.toolbars.tables;

import com.marlontrujillo.eru.comm.connection.Connection;
import com.marlontrujillo.eru.comm.device.Address;
import com.marlontrujillo.eru.comm.device.Device;
import com.marlontrujillo.eru.gui.App;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by mtrujillo on 8/8/17.
 *
 * Hay un problema con Hibernate vs JavaFX.
 * Lado del bean:
 *  1- Si addresses es ListProperty   => ListProperty cambia la instancia del persistence context y Hibernate no lo soporta.
 *  2- Si addresses es observableList => ""
 *  3 - Si es List = Es aceptado porque la instancia se mantiene
 *
 * En el lado de la celda:
 *  1- Si es List, los cambios en la celda no son reflejados en el bean.
 *  2- Si es observableList (refleja cambios en List), hay que envolverlo en ObjectProperty o ListProperty. No refleja cambios
 *      a menos que el bean sea un property tambien.
 *
 * Si los dos son property. Funcionan los cambios pero Hibernate no tolera que se cambie la instancia en el bean y esto es
 * hecho cuando en el bean se usa un property.
 *
 * Puede ser que se usen properties en el bean (para hacer funcionar la celda de tabla) y se soluciona el cambio de instancia
 * en la lista para Hibernate
 *
 */
public class DeviceTable extends EruTable<Device> {

    public DeviceTable(List<Device> items) {
        super(items);

        // **** Columns **** //
        TableColumn<Device, String> groupColumn             = new TableColumn<>("Group");
        TableColumn<Device, String> nameColumn              = new TableColumn<>("Name");
        TableColumn<Device, Integer> unitIdentifierColumn   = new TableColumn<>("ID");
        TableColumn<Device, String> statusColumn            = new TableColumn<>("Status");
        TableColumn<Device, Integer> retriesColumn          = new TableColumn<>("Retries");
        TableColumn<Device, Boolean> enabledColumn          = new TableColumn<>("Enabled");
        TableColumn<Device, List<Address>> addressesColumn  = new TableColumn<>("Addresses");
        TableColumn<Device, Boolean> zeroBasedColumn        = new TableColumn<>("Zero based");
        TableColumn<Device, Connection> connectionColumn    = new TableColumn<>("Connection");

        groupColumn.setCellValueFactory(param -> param.getValue().groupNameProperty());
        groupColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        groupColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.08));

        nameColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.10));
        nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        unitIdentifierColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.05));
        unitIdentifierColumn.setCellValueFactory(param -> param.getValue().unitIdentifierProperty().asObject());
        unitIdentifierColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object != null ? object.toString() : "";
            }

            @Override
            public Integer fromString(String string) {
                return Integer.valueOf(string);
            }
        }));

        statusColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.11));
        statusColumn.setCellValueFactory(param -> param.getValue().statusProperty());
        statusColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        retriesColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.05));
        retriesColumn.setCellValueFactory(param -> param.getValue().retriesProperty().asObject());
        retriesColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object != null ? object.toString() : "";
            }

            @Override
            public Integer fromString(String string) {
                return Integer.valueOf(string);
            }
        }));

        enabledColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.05));
        enabledColumn.setCellValueFactory(param -> param.getValue().enabledProperty());
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));

        addressesColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.33));
        addressesColumn.setCellValueFactory(new PropertyValueFactory<>("addresses"));
        addressesColumn.setCellFactory(param -> new AddressTableCell());

        zeroBasedColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.08));
        zeroBasedColumn.setCellValueFactory(param -> param.getValue().zeroBasedProperty());
        zeroBasedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));

        connectionColumn.prefWidthProperty().bind(this.widthProperty().multiply(0.14));
        connectionColumn.setCellValueFactory(param -> param.getValue().connectionProperty());
        connectionColumn.setCellFactory(ChoiceBoxTableCell.forTableColumn(
                FXCollections.observableList(App.getSingleton().getProject().getConnections())
        ));

        // **** General **** //
        this.getColumns().addAll(
                groupColumn,
                nameColumn,
                unitIdentifierColumn,
                statusColumn,
                retriesColumn,
                enabledColumn,
                addressesColumn,
                zeroBasedColumn,
                connectionColumn
        );

        this.setEditable(true);
        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @Override
    public void addNewItem() {
        Device newDevice = new Device();
        newDevice.setName("New device");
        newDevice.setGroupName("Devices");
        this.items.add(newDevice);
        this.getSelectionModel().clearSelection();
        this.getSelectionModel().select(newDevice);

        // *******************************************************************************
        // Implemented to solve : https://javafx-jira.kenai.com/browse/RT-32091
        // When a new object is added to the table, a new filteredList has to be created
        // and the items updated, because the filteredList is non-editable. So, despite the
        // filtered List is setted to the tableview, a list is used in the background. The
        // filtered list is only used to be able to filter using the textToFilter.
        //
        //Wrap ObservableList into FilteredList
        super.filteredItems = new FilteredList<>(this.items);
        super.setItems(this.filteredItems);

        // Check if a textToFilter is setted
        if (super.textToFilter != null){
            setTextToFilter(textToFilter);
        }
        // *******************************************************************************
    }

    @Override
    public void setTextToFilter(StringProperty textToFilter) {
        textToFilter.addListener(observable ->
                this.filteredItems.setPredicate(device ->
                        (textToFilter.getValue() == null
                                || textToFilter.getValue().isEmpty()
                                || device.getName().startsWith(textToFilter.getValue())
                                || device.getGroupName().startsWith(textToFilter.getValue()))
                )
        );
    }
}

class AddressTableCell extends TableCell<Device, List<Address>> {

    @Override
    protected void updateItem(List<Address> item, boolean empty) {
        super.updateItem(item, empty);
        updateViewMode();
    }

    private void updateViewMode() {
        setGraphic(null);
        setText(null);
        if(isEditing()){
            VBox box = new VBox();
            TableView<Address> addressesTableView = new TableView<>();
            addressesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            TableColumn<Address, String> typeColumn = new TableColumn<>("Type");
            typeColumn.setCellValueFactory(param -> param.getValue().getAddressPK().dataModelProperty().asString());
            typeColumn.prefWidthProperty().bind(addressesTableView.widthProperty().multiply(0.2));

            TableColumn<Address, String> addressColumn = new TableColumn<>("Address");
            addressColumn.setCellValueFactory(param -> param.getValue().getAddressPK().idProperty().asString());
            addressColumn.prefWidthProperty().bind(addressesTableView.widthProperty().multiply(0.2));

            TableColumn<Address, String> valueColumn = new TableColumn<>("Value");
            valueColumn.setCellValueFactory(param -> param.getValue().currentValueProperty().asString());
            valueColumn.prefWidthProperty().bind(addressesTableView.widthProperty().multiply(0.2));

            TableColumn<Address, String> statusColumn = new TableColumn<>("Status");
            statusColumn.setCellValueFactory(param -> param.getValue().statusProperty());
            statusColumn.prefWidthProperty().bind(addressesTableView.widthProperty().multiply(0.2));

            TableColumn<Address, Timestamp> timestampColumn = new TableColumn<>("Value");
            timestampColumn.setCellValueFactory(param -> param.getValue().timestampProperty());
            timestampColumn.prefWidthProperty().bind(addressesTableView.widthProperty().multiply(0.2));

            addressesTableView.getColumns().addAll(typeColumn, addressColumn, valueColumn, statusColumn, timestampColumn);

            TextField startAddressTextField = new TextField();
            startAddressTextField.setPromptText("Start");
            TextField endAddressTextField = new TextField();
            endAddressTextField.setPromptText("End");
            ChoiceBox<Address.DataModel> typeChoiceBox = new ChoiceBox<>();

            if (getItem() != null){
                addressesTableView.getItems().addAll(getItem());
                typeChoiceBox.getItems().addAll(Address.DataModel.values());
            }

            Button addAddressButton = new Button("Add");
            addAddressButton.setOnAction(event -> {
                try{
                    final int firstAddress = Integer.parseInt(startAddressTextField.getText());
                    final int lastAddress = Integer.parseInt(endAddressTextField.getText());
                    if (firstAddress > lastAddress) throw new Exception("Last Address is minor than first.");
                    final Address.DataModel dataType = typeChoiceBox.getSelectionModel().getSelectedItem();

                    for (int i = firstAddress; i <= lastAddress; i++) {
                        boolean isAlreadyInTable = false;
                        for(Address address : addressesTableView.getItems()){
                            if((address.getAddressPK().getId() == i) && (address.getAddressPK().getDataModel().equals(dataType))){
                                isAlreadyInTable = true;
                                break;
                            }
                        }
                        if(!isAlreadyInTable){
                            Address newAddress = new Address();
                            newAddress.getAddressPK().setId(i);
                            newAddress.getAddressPK().setDataModel(dataType);
                            addressesTableView.getItems().add(newAddress);
                        }
                    }
                    // Clear the new addresses fields
                    startAddressTextField.clear();
                    endAddressTextField.clear();

                } catch (Exception e){
                    e.printStackTrace();
                }
            });

            Button deleteAddressButton = new Button("Remove");
            deleteAddressButton.setOnAction(event -> {
                final int CURRENT_INDEX = addressesTableView.getSelectionModel().getSelectedIndex();
                addressesTableView.getItems().removeAll(addressesTableView.getSelectionModel().getSelectedItems());
                addressesTableView.getSelectionModel().select(CURRENT_INDEX -1);
            });

            Button okButton = new Button("OK");
            okButton.setOnAction(event -> {
                commitEdit(addressesTableView.getItems());
                getItem().clear();
                getItem().addAll(addressesTableView.getItems());
            });

            ToolBar toolBar = new ToolBar(
                    startAddressTextField,
                    endAddressTextField,
                    typeChoiceBox,
                    new HBox(addAddressButton,
                            deleteAddressButton,
                            okButton));
            toolBar.setOrientation(Orientation.VERTICAL);

            box.getChildren().addAll(addressesTableView, toolBar);
            setGraphic(box);
        } else {
            if(getItem() != null) {
                setText(getItem().toString());
            }
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        updateViewMode();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem().toString());
        setGraphic(null);
    }

    @Override
    public void commitEdit(List<Address> newValue) {
        super.commitEdit(newValue);
    }
}