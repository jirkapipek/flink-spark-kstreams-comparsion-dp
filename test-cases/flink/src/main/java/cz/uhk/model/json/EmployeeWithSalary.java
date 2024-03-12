package cz.uhk.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmployeeWithSalary {
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

    @JsonProperty("gender")
    public String gender;

    @JsonProperty("email")
    public String email;

    @JsonProperty("salary")
    public Double salary;
}