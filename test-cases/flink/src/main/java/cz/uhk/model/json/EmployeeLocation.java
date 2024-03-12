package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmployeeLocation {
    @JsonProperty("employee_id")
    public int employee_id;

    @JsonProperty("lab")
    public String lab;

    @JsonProperty("department_id")
    public int department_id;

    @JsonProperty("arrival_date")
    public int arrival_date;

}

