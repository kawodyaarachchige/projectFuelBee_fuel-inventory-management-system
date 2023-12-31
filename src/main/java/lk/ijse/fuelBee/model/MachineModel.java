package lk.ijse.fuelBee.model;

import lk.ijse.fuelBee.controller.ProfitFormController;
import lk.ijse.fuelBee.db.Dbconnection;
import lk.ijse.fuelBee.dto.MachineDto;
import lk.ijse.fuelBee.dto.PaymentDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MachineModel {
    public static boolean saveMachine(MachineDto machineDto) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql = "INSERT INTO Machine VALUES(?,?,?,?,?,?,?)";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setString(1, machineDto.getMachineId());
        pstm.setString(2, machineDto.getFuelId());
        pstm.setString(3, machineDto.getFuelType());
        pstm.setString(4, machineDto.getAvailability());
        pstm.setInt(5, machineDto.getStartFuelAmount());
        pstm.setInt(6, machineDto.getEndFuelAmount());
        pstm.setDate(7, machineDto.getDate());

        if (pstm.executeUpdate() > 0) {
            return true;
        }else{
            return false;
        }
    }

    public static boolean deleteMachine(String id) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql="DELETE FROM Machine WHERE machine_id=?";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setString(1,id);

        if(pstm.executeUpdate()>0){
            return true;
        }else{
            return false;
        }
    }

    public static boolean updateMachine(MachineDto machineDto) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql="UPDATE Machine SET machine_id=?,fuel_id=?,type=?,availability=?,start_fuel_amount=?,day_end_fuel_amount=?,date=? WHERE machine_id=?";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setString(1, machineDto.getMachineId());
        pstm.setString(2, machineDto.getFuelId());
        pstm.setString(3, machineDto.getFuelType());
        pstm.setString(4, machineDto.getAvailability());
        pstm.setInt(5, machineDto.getStartFuelAmount());
        pstm.setInt(6, machineDto.getEndFuelAmount());
        pstm.setDate(7, machineDto.getDate());
        pstm.setString(8, machineDto.getMachineId());

        if(pstm.executeUpdate()>0){
            return true;
        }else{
            return false;
        }
    }

    public static ArrayList<MachineDto> getAllMachine() throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql = "SELECT * FROM Machine";
        PreparedStatement pstm = connection.prepareStatement(sql);
        ResultSet rst = pstm.executeQuery();

        ArrayList<MachineDto> machines = new ArrayList<>();
        while(rst.next()){
            machines.add(new MachineDto(
                    rst.getString(1),
                    rst.getString(2),
                    rst.getString(3),
                    rst.getString(4),
                    rst.getInt(5),
                    rst.getInt(6),
                    rst.getDate(7)
            ));
        }return machines;
    }

    public static boolean changeDayEndFuelByWaste(String id,int waste) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql="UPDATE Machine SET day_end_fuel_amount=day_end_fuel_amount-? WHERE fuel_id=?";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setInt(1,waste);
        pstm.setString(2,id);

        if(pstm.executeUpdate()>0){
            return true;
        }else{
            return false;
        }
    }

    public static boolean checkDayEndAmounts(String id,int amount) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql = "SELECT * FROM Machine WHERE fuel_id=? AND day_end_fuel_amount = ?";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setString(1, id);
        pstm.setInt(2, amount);

        ResultSet rst = pstm.executeQuery();
        while (rst.next()) {
            return true;
        }
        return false;
    }

    public static ArrayList<MachineDto> getCapacityLowFuels() throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql="SELECT * FROM Machine WHERE day_end_fuel_amount < 1000";
        PreparedStatement pstm = connection.prepareStatement(sql);
        ResultSet rst = pstm.executeQuery();
        ArrayList<MachineDto> lowFuelMachines = new ArrayList<>();
        while (rst.next()){
           lowFuelMachines.add(new MachineDto(
                   rst.getString(1),
                   rst.getString(2),
                   rst.getString(3),
                   rst.getString(4),
                   rst.getInt(5),
                   rst.getInt(6),
                   rst.getDate(7)
           ));
        }
        return lowFuelMachines;
    }

    public static boolean confirmMachineUsage(MachineDto machineDto) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        try {
            connection.setAutoCommit(false);

            String sql = "INSERT INTO Machine VALUES(?,?,?,?,?,?,?)";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1, machineDto.getMachineId());
            pstm.setString(2, machineDto.getFuelId());
            pstm.setString(3, machineDto.getFuelType());
            pstm.setString(4, machineDto.getAvailability());
            pstm.setInt(5, machineDto.getStartFuelAmount());
            pstm.setInt(6, machineDto.getEndFuelAmount());
            pstm.setDate(7, machineDto.getDate());
            int isSaved = pstm.executeUpdate();

            String sql1="INSERT INTO Income VALUES(?,?,?)";
            PreparedStatement pstm1 = connection.prepareStatement(sql1);
            pstm1.setString(1, machineDto.getMachineId());
            //pstm1.setString(1, ProfitFormController.generateIncomeId());
            Double fuelPrice = FuelModel.getFuelPriceByName(machineDto.getFuelType());
            Double soldFuelAmount= (double) ((machineDto.getStartFuelAmount())-(machineDto.getEndFuelAmount()));
            //System.out.println("sold Fuel Amount is : "+soldFuelAmount+"L and fuel price is : Rs."+fuelPrice);
            pstm1.setDouble(2,(soldFuelAmount*fuelPrice));
            pstm1.setDate(3, machineDto.getDate());
            int isIncomeSaved = pstm1.executeUpdate();


            if ((isSaved > 0) && (isIncomeSaved > 0)) {
                connection.commit();
                return true;
            } else {
                connection.rollback();
                System.out.println("Failed to Complete the Machine Usage save & Income save");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    public static boolean deleteIncome(String id) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        try{
            connection.setAutoCommit(false);

            String sql="DELETE FROM Machine WHERE machine_id=?";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1,id);
            int isDeleted = pstm.executeUpdate();

            String sql1="DELETE FROM Income WHERE income_id=?";
            PreparedStatement pstm1 = connection.prepareStatement(sql1);
            pstm1.setString(1,id);
            int isDeleted1 = pstm1.executeUpdate();

            if ((isDeleted > 0) && (isDeleted1 > 0)) {
                connection.commit();
                return true;
            }else{
                connection.rollback();
                return false;
            }
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static boolean updateMachineSpecs(MachineDto machineDto) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        try{
            connection.setAutoCommit(false);

            String sql="UPDATE Machine SET machine_id=?,fuel_id=?,type=?,availability=?,start_fuel_amount=?,day_end_fuel_amount=?,date=? WHERE machine_id=?";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1, machineDto.getMachineId());
            pstm.setString(2, machineDto.getFuelId());
            pstm.setString(3, machineDto.getFuelType());
            pstm.setString(4, machineDto.getAvailability());
            pstm.setInt(5, machineDto.getStartFuelAmount());
            pstm.setInt(6, machineDto.getEndFuelAmount());
            pstm.setDate(7, machineDto.getDate());
            pstm.setString(8, machineDto.getMachineId());

            int isUpdated = pstm.executeUpdate();

            String sql1="UPDATE Income SET income_id=?,amount=?,date=? WHERE income_id=?";
            PreparedStatement pstm1 = connection.prepareStatement(sql1);
            pstm1.setString(1, machineDto.getMachineId());
            Double fuelPrice = FuelModel.getFuelPriceByName(machineDto.getFuelType())*(machineDto.getStartFuelAmount()-machineDto.getEndFuelAmount());
            pstm1.setDouble(2,fuelPrice);
            pstm1.setDate(3, machineDto.getDate());
            pstm1.setString(4, machineDto.getMachineId());

            int isIncomeUpdated = pstm1.executeUpdate();

            if((isUpdated > 0) && (isIncomeUpdated > 0)) {
                connection.commit();
                return true;
            }else{
                connection.rollback();
                return false;
            }

        }catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    public static MachineDto searchMachine(String id) throws SQLException {
        Connection connection = Dbconnection.getInstance().getConnection();
        String sql = "SELECT * FROM Machine WHERE machine_id=?";
        PreparedStatement pstm = connection.prepareStatement(sql);
        pstm.setString(1, id);
        ResultSet rst = pstm.executeQuery();
        if(rst.next()){
            return new MachineDto(
                    rst.getString(1),
                    rst.getString(2),
                    rst.getString(3),
                    rst.getString(4),
                    rst.getInt(5),
                    rst.getInt(6),
                    rst.getDate(7)
            );
        }else{
            return null;
        }
    }

}
