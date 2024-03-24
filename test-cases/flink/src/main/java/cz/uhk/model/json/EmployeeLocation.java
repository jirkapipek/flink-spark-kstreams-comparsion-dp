package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmployeeLocation {
    @JsonProperty("employee_id")
    public int employeeId;

    @JsonProperty("lab")
    public String lab;

    @JsonProperty("department_id")
    public int departmentId;

    @JsonProperty("arrival_date")
    public int arrivalDate;

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(int arrivalDate) {
        this.arrivalDate = arrivalDate;
    }
}

