package lk.ijse.fuelBee.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import lk.ijse.fuelBee.Mail;
import lk.ijse.fuelBee.db.Dbconnection;
import lk.ijse.fuelBee.dto.AdminDto;
import lk.ijse.fuelBee.dto.OrderDto;
import lk.ijse.fuelBee.dto.PaymentDto;
import lk.ijse.fuelBee.dto.SupplierDto;
import lk.ijse.fuelBee.dto.tm.EmployeeTm;
import lk.ijse.fuelBee.dto.tm.PaymentTm;
import lk.ijse.fuelBee.model.AdminModel;
import lk.ijse.fuelBee.model.OrderModel;
import lk.ijse.fuelBee.model.PaymentModel;
import lk.ijse.fuelBee.model.SupplierModel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;

import java.io.File;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

public class PaymentFormController {
    public TextField txtPayId;
    
    //public TextField txtAmount;

    public DatePicker dpDate;
    public TextField txtStatus;
    public ComboBox cmbAdminEmail;
    public TableView<PaymentTm> tblPayments;
    public TableColumn<?,?> colId;
    public TableColumn<?,?> colAdmin;
    //public TableColumn<?,?> colSupplier;
    public TableColumn<?,?> colMethod;
    public TableColumn<?,?> colAmount;
    public TableColumn<?,?> colDate;
    public TableColumn<?,?> colStatus;
    public TableColumn<?,?> colOrderId;
    public ComboBox cmbOrderId;

    public ComboBox cmbSupEmail;
    public Text txtAmount1;
    public TextField txtMethod;
    public TextField txtAmount;

    public void initialize() throws SQLException {
        getAllSuppliers();
        getAllOrderId();
        getAllAdminEmail();
        getAllPayments();
        setCellValueFactory();

        cmbOrderId.setOnAction(event -> {
            try {
                if (cmbOrderId.getSelectionModel().getSelectedItem() != null) {
                    OrderDto orderDetails = OrderModel.getOrderDetails(cmbOrderId.getSelectionModel().getSelectedItem().toString());

                    if (orderDetails != null) {
                        txtAmount.setText(String.valueOf(orderDetails.getPrice()));
                    } else {
                        txtAmount.setText("0.0");
                    }
                } else {
                    txtAmount.setText("0.0");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        tblPayments.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() >= 0) {
                PaymentTm selectedPayment = tblPayments.getItems().get(newValue.intValue());
                txtPayId.setText(selectedPayment.getPaymentId());
                txtMethod.setText(selectedPayment.getMethod());
                txtAmount.setText(String.valueOf(selectedPayment.getAmount()));
                //dpDate.setValue(LocalDate.parse(selectedPayment.getDate().toString()));
                txtStatus.setText(selectedPayment.getStatus());
                cmbOrderId.setValue(selectedPayment.getOrderId());
                cmbSupEmail.setValue(selectedPayment.getSup_email());
                dpDate.setValue(LocalDate.parse(selectedPayment.getDate().toString()));
                cmbAdminEmail.setValue(selectedPayment.getEmail());
            }
        });

    }
    public void setCellValueFactory(){
        colId.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        colAdmin.setCellValueFactory(new PropertyValueFactory<>("email"));
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("method"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }
    public void btnDeleteOnAction(ActionEvent actionEvent) {
        String payId = txtPayId.getText();
        try {
            boolean isDeleted = PaymentModel.deleteOutcome(payId);
            if (isDeleted) {
                new Alert(Alert.AlertType.CONFIRMATION, "Payment Deleted").show();
                getAllPayments();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR, "Payment Not Deleted").show();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void btnClearOnAction(ActionEvent actionEvent) {
        clearFields();
    }

    public void btnOnUpdateAction(ActionEvent actionEvent) {
        String payId = txtPayId.getText();
        String email = cmbAdminEmail.getValue().toString();
        String supEmail=cmbSupEmail.getValue().toString();
        String orderId = cmbOrderId.getValue().toString();
        String method = txtMethod.getText();
        double amount = Double.parseDouble(txtAmount.getText());
        Date date = Date.valueOf(dpDate.getValue());
        String status = txtStatus.getText();

        PaymentDto paymentDtoDto = new PaymentDto(payId,email,supEmail,orderId, method, amount, date, status);
        try {
            boolean isUpdated = PaymentModel.updatePayment(paymentDtoDto);
            if (isUpdated) {
                new Alert(Alert.AlertType.CONFIRMATION, "Payment Updated").show();
                getAllPayments();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR, "Payment Not Updated").show();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

    }


    public void btnSaveOnAction(ActionEvent actionEvent) throws SQLException {
        String paymentId = txtPayId.getText();
        String orderId=cmbOrderId.getValue().toString();
        String supEmail=cmbSupEmail.getValue().toString();
        String email = cmbAdminEmail.getValue().toString();
        String method = txtMethod.getText();
        double amount = Double.parseDouble(txtAmount.getText());
        Date date = Date.valueOf(dpDate.getValue());
        String status = txtStatus.getText();


        PaymentDto paymentDtoDto = new PaymentDto(paymentId,email,supEmail,orderId,method, amount, date, status);
        try {
            boolean isPaymentConfirmed = PaymentModel.confirmPayment(paymentDtoDto);
            if (isPaymentConfirmed) {
                new Alert(Alert.AlertType.CONFIRMATION, "Payment Saved").show();
                getAllPayments();
                clearFields();

                Mail mail = new Mail();
                mail.setMsg("\n\nFuel bee made a payment of " + amount + " for order id " + orderId + "\n \nPlease send invoice to"+email+"\n\nThank You");
                mail.setTo(supEmail);
                mail.setSubject("Fuel Bee");

                Thread thread = new Thread(mail);
                thread.start();
            }else{
                new Alert(Alert.AlertType.ERROR, "Payment Not Saved").show();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

    }
    public String generatePaymentId() {
        String prefix = "PAY";
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
        String timestampString = dateFormat.format(new Date(timestamp));
        Random random = new Random();
        int randomComponent = random.nextInt(1000);
        String paymentId = prefix + timestampString + randomComponent;
        return paymentId;
    }
    public void getAllSuppliers() throws SQLException {
        ObservableList<String> obList = FXCollections.observableArrayList();
        ArrayList<SupplierDto> allSuppliers = SupplierModel.getAllSuppliers();
        for (SupplierDto supplierDto : allSuppliers) {
            obList.add(supplierDto.getSup_email());
        }
        cmbSupEmail.setItems(obList);
    }

    public void getAllOrderId() throws SQLException {
        ObservableList<String> obList = FXCollections.observableArrayList();
        ArrayList<OrderDto> allOrders = OrderModel.getAllOrders();
        for (OrderDto orderDto : allOrders) {
            if(orderDto.getStatus().equals("NOT PAID")){
                obList.add(orderDto.getOrderId());
            }
        }
        cmbOrderId.setItems(obList);
    }
    public void getAllAdminEmail() throws SQLException {
        ObservableList<String> obList = FXCollections.observableArrayList();
        ArrayList<AdminDto> allAdmins = AdminModel.getAllAdmins();
        for (AdminDto adminDto : allAdmins) {
            obList.add(adminDto.getEmail());
        }
        cmbAdminEmail.setItems(obList);
    }
    public void getAllPayments() throws SQLException {
        ObservableList<PaymentTm> obList = FXCollections.observableArrayList();
        ArrayList<PaymentDto> allPayments = PaymentModel.getAllPayments();
        for (PaymentDto paymentDto : allPayments) {
            obList.add(new PaymentTm(
                    paymentDto.getPaymentId(),
                    paymentDto.getEmail(),
                    paymentDto.getOrderId(),
                    paymentDto.getMethod(),
                    paymentDto.getAmount(),
                    paymentDto.getDate(),
                    paymentDto.getStatus(),
                    paymentDto.getSup_email()
            ));
        }
        tblPayments.setItems(obList);
        tblPayments.refresh();
    }

    public void txtPayIdOnAction(MouseEvent mouseEvent) {
        txtPayId.setText(generatePaymentId());
    }

    public void clearFields(){
        txtPayId.clear();
        cmbAdminEmail.getSelectionModel().clearSelection();
        txtMethod.clear();
        txtAmount.clear();
        dpDate.getEditor().clear();
        txtStatus.clear();
        cmbOrderId.getSelectionModel().clearSelection();;
        cmbSupEmail.getSelectionModel().clearSelection();;
    }

    public void btnReportOnAction(ActionEvent actionEvent) throws JRException, SQLException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/report/paymentHistory1.jrxml");
        JasperDesign load = JRXmlLoader.load(resourceAsStream);
        JasperReport jasperReport = JasperCompileManager.compileReport(load);
        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                null,
                Dbconnection.getInstance().getConnection());
        JasperViewer.viewReport(jasperPrint, false);

        String filepath = "/home/kitty99/IdeaProjects/paymentRecipt/"+txtPayId+".pdf";

        JasperExportManager.exportReportToPdfFile(jasperPrint, filepath);
        System.out.println("saved");

        Mail mail = new Mail();
        mail.setMsg("Payment History");
        mail.setTo("projectfuelbee@gmail.com");
        mail.setSubject("Fuel Bee");
        mail.setFile(new File(filepath));

        Thread thread = new Thread(mail);
        thread.start();

    }
}
