@FetchProfile(
    name = "employee.projects",
    fetchOverrides = {
        @FetchOverride(
            entity = Employee.class,
            association = "projects",
            mode = JOIN
        )
    }
)