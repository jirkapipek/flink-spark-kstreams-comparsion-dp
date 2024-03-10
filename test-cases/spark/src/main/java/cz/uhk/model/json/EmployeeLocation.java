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

}

