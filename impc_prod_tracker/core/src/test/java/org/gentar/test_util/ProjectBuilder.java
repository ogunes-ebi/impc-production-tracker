package org.gentar.test_util;

import org.gentar.biology.project.Project;
import org.gentar.biology.project.assignment.AssignmentStatus;
import org.gentar.biology.project.search.filter.ProjectFilter;

public class ProjectBuilder
{
    private AssignmentStatus assignmentStatus;
    private boolean withdrawn;

    public static ProjectBuilder getInstance()
    {
        return new ProjectBuilder();
    }

    public Project build()
    {
        Project project = new Project();
        project.setWithdrawn(withdrawn);
        project.setAssignmentStatus(assignmentStatus);
        return project;
    }

    public ProjectBuilder withProjectAssignmentStatus(String assignmentStatusName)
    {
        AssignmentStatus assignmentStatus = new AssignmentStatus();
        assignmentStatus.setName(assignmentStatusName);
        this.assignmentStatus = assignmentStatus;
        return this;
    }

    public ProjectBuilder withWithdrawn(boolean withdrawn)
    {
        this.withdrawn = withdrawn;
        return this;
    }
}
