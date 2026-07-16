package com.atlas.modules.labels.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.labels.dto.CreateLabelRequest;
import com.atlas.modules.labels.dto.LabelResponse;
import com.atlas.modules.labels.entity.Label;
import com.atlas.modules.labels.mapper.LabelMapper;
import com.atlas.modules.labels.repository.LabelRepository;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public LabelService(
            LabelRepository labelRepository,
            ProjectAuthorizationService projectAuthorizationService) {
        this.labelRepository = labelRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public LabelResponse create(UUID projectId, UUID userId, CreateLabelRequest request) {
        Project project = projectAuthorizationService.requireVisibleProject(projectId, userId);
        projectAuthorizationService.requireCanManageTasks(projectId, userId);

        String name = request.name().trim();
        if (labelRepository.existsByOrganizationIdAndProjectIdAndNameIgnoreCase(
                project.getOrganizationId(), projectId, name)) {
            throw new ApiException(ErrorCode.CONFLICT, "Label name is already taken", HttpStatus.CONFLICT);
        }

        Label label = new Label();
        label.setOrganizationId(project.getOrganizationId());
        label.setProjectId(projectId);
        label.setName(name);
        label.setColor(request.color().toUpperCase());
        return LabelMapper.toResponse(labelRepository.save(label));
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> listForProject(UUID projectId, UUID userId) {
        projectAuthorizationService.requireVisibleProject(projectId, userId);
        return labelRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(LabelMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID labelId, UUID userId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));

        if (label.getProjectId() != null) {
            projectAuthorizationService.requireCanManageTasks(label.getProjectId(), userId);
        } else {
            throw new ResourceNotFoundException("Label not found");
        }

        labelRepository.delete(label);
    }

    public Label findUsableLabel(UUID labelId, UUID organizationId, UUID projectId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
        if (!label.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("Label not found");
        }
        if (label.getProjectId() != null && !label.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Label not found");
        }
        return label;
    }
}
