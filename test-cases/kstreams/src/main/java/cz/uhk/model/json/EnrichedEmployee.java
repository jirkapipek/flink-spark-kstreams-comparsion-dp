package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnrichedEmployee {
    @JsonProperty("employee_id")
    public int employeeId;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("lab")
    public String lab;
}