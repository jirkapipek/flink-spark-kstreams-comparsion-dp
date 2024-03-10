package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Employee {
    @JsonProperty("employee_id")
    public int employeeId;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("age")
    public int age;

    @JsonProperty("ssn")
    public String ssn;

    @JsonProperty("hourly_rate")
    public double hourlyRate;

    @JsonProperty("gender")
    public String gender;

    @JsonProperty("email")
    public String email;
}