package com.atlas.modules.tasks;

import com.atlas.modules.attachments.repository.AttachmentRepository;
import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.comments.repository.CommentRepository;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.labels.repository.LabelRepository;
import com.atlas.modules.milestones.repository.MilestoneRepository;
import com.atlas.modules.organizations.OrganizationIntegrationTestSupport;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.atlas.modules.tasks.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerIntegrationTest extends OrganizationIntegrationTestSupport {

    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanData() {
        attachmentRepository.deleteAll();
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        milestoneRepository.deleteAll();
        labelRepository.deleteAll();
        invitationRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        projectRepository.deleteAll();
        membershipRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void createsTaskWithSequentialKey() throws Exception {
        String token = registerAndLogin("tasker@example.com", "Task User");
        UUID organizationId = createOrganization(token, "task-org", "Task Org");
        UUID projectId = createProject(token, organizationId, "ATL");

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First task\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.key").value("ATL-1"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void listsAndFiltersTasks() throws Exception {
        String token = registerAndLogin("filter@example.com", "Filter User");
        UUID organizationId = createOrganization(token, "filter-org", "Filter Org");
        UUID projectId = createProject(token, organizationId, "FLT");

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Filter me\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(taskId.toString()));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks?status=DONE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void setsCompletedAtWhenMarkedDone() throws Exception {
        String token = registerAndLogin("done@example.com", "Done User");
        UUID organizationId = createOrganization(token, "done-org", "Done Org");
        UUID projectId = createProject(token, organizationId, "DON");

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Complete me\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    @Test
    void softDeletesTask() throws Exception {
        String token = registerAndLogin("delete@example.com", "Delete User");
        UUID organizationId = createOrganization(token, "del-org", "Del Org");
        UUID projectId = createProject(token, organizationId, "DEL");

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Delete me\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void addsAndRemovesAssignee() throws Exception {
        String ownerToken = registerAndLogin("owner2@example.com", "Owner Two");
        UUID organizationId = createOrganization(ownerToken, "assign-org", "Assign Org");
        UUID projectId = createProject(ownerToken, organizationId, "ASN");

        String memberToken = registerAndLogin("member2@example.com", "Member Two");
        UUID memberUserId = userIdFromToken(memberToken);
        inviteAndAccept(ownerToken, organizationId, "member2@example.com", memberToken);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberUserId + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Assign task\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/assignees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberUserId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeIds", hasItem(memberUserId.toString())));

        mockMvc.perform(delete("/api/v1/tasks/" + taskId + "/assignees/" + memberUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeIds.length()").value(0));
    }

    @Test
    void createsMilestoneLabelAndAttachesLabelToTask() throws Exception {
        String token = registerAndLogin("ml@example.com", "ML User");
        UUID organizationId = createOrganization(token, "ml-org", "ML Org");
        UUID projectId = createProject(token, organizationId, "MLB");

        MvcResult milestoneResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/milestones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sprint 1\",\"startDate\":\"2026-07-01\",\"dueDate\":\"2026-07-31\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID milestoneId = UUID.fromString(objectMapper.readTree(milestoneResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        MvcResult labelResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bug\",\"color\":\"#FF0000\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID labelId = UUID.fromString(objectMapper.readTree(labelResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Labelled task\",\"milestoneId\":\"" + milestoneId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/labels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labelId\":\"" + labelId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labelIds", hasItem(labelId.toString())));
    }

    @Test
    void createsCommentOnTask() throws Exception {
        String token = registerAndLogin("comment@example.com", "Comment User");
        UUID organizationId = createOrganization(token, "cmt-org", "Comment Org");
        UUID projectId = createProject(token, organizationId, "CMT");

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Discuss me\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Looks good @teammate\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("Looks good @teammate"));

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void deniesCrossTenantTaskAccess() throws Exception {
        String ownerToken = registerAndLogin("tenant-a@example.com", "Tenant A");
        UUID organizationA = createOrganization(ownerToken, "tenant-a", "Tenant A");
        UUID projectId = createProject(ownerToken, organizationA, "TNA");

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Secret task\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        String outsiderToken = registerAndLogin("tenant-b@example.com", "Tenant B");
        createOrganization(outsiderToken, "tenant-b", "Tenant B");

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    private UUID createProject(String token, UUID organizationId, String key) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Test Project", key, null, "ORG_WIDE");
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    private void inviteAndAccept(String ownerToken, UUID organizationId, String email, String inviteeToken)
            throws Exception {
        subscriptionRepository.findByOrganizationId(organizationId).ifPresent(sub -> {
            sub.setSeats(5);
            subscriptionRepository.save(sub);
        });

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invitationId = UUID.fromString(objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk());
    }

    private UUID userIdFromToken(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(me.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    private String registerAndLogin(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password1234", fullName))));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password1234"))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private UUID createOrganization(String token, String slug, String name) throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest(name, slug, "UTC", "US");
        MvcResult result = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }
}
