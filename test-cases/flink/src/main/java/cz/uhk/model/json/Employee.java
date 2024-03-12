package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Employee {
    @JsonProperty("employee_id")
    public int employee_id;

    @JsonProperty("first_name")
    public String first_name;

    @JsonProperty("last_name")
    public String last_name;

    @JsonProperty("age")
    public int age;

    @JsonProperty("ssn")
    public String ssn;

    @JsonProperty("hourly_rate")
    public double hourly_rate;

    @JsonProperty("gender")
    public String gender;

    @JsonProperty("email")
    public String email;
}