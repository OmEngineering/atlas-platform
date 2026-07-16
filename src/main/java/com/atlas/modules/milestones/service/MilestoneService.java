package com.atlas.modules.milestones.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.milestones.dto.CreateMilestoneRequest;
import com.atlas.modules.milestones.dto.MilestoneResponse;
import com.atlas.modules.milestones.dto.UpdateMilestoneRequest;
import com.atlas.modules.milestones.entity.Milestone;
import com.atlas.modules.milestones.entity.MilestoneStatus;
import com.atlas.modules.milestones.mapper.MilestoneMapper;
import com.atlas.modules.milestones.repository.MilestoneRepository;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public MilestoneService(
            MilestoneRepository milestoneRepository,
            ProjectAuthorizationService projectAuthorizationService) {
        this.milestoneRepository = milestoneRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public MilestoneResponse create(UUID projectId, UUID userId, CreateMilestoneRequest request) {
        projectAuthorizationService.requireCanManageTasks(projectId, userId);
        validateDates(request.startDate(), request.dueDate());

        Milestone milestone = new Milestone();
        milestone.setProjectId(projectId);
        milestone.setName(request.name().trim());
        milestone.setDescription(request.description());
        milestone.setStartDate(request.startDate());
        milestone.setDueDate(request.dueDate());
        milestone.setStatus(parseStatus(request.status()));
        return MilestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> listForProject(UUID projectId, UUID userId) {
        projectAuthorizationService.requireVisibleProject(projectId, userId);
        return milestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(MilestoneMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MilestoneResponse get(UUID milestoneId, UUID userId) {
        Milestone milestone = findMilestone(milestoneId);
        projectAuthorizationService.requireVisibleProject(milestone.getProjectId(), userId);
        return MilestoneMapper.toResponse(milestone);
    }

    @Transactional
    public MilestoneResponse update(UUID milestoneId, UUID userId, UpdateMilestoneRequest request) {
        Milestone milestone = findMilestone(milestoneId);
        projectAuthorizationService.requireCanManageTasks(milestone.getProjectId(), userId);

        if (request.name() != null) {
            milestone.setName(request.name().trim());
        }
        if (request.description() != null) {
            milestone.setDescription(request.description());
        }
        LocalDate startDate = request.startDate() != null ? request.startDate() : milestone.getStartDate();
        LocalDate dueDate = request.dueDate() != null ? request.dueDate() : milestone.getDueDate();
        validateDates(startDate, dueDate);
        if (request.startDate() != null) {
            milestone.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            milestone.setDueDate(request.dueDate());
        }
        if (request.status() != null) {
            milestone.setStatus(parseStatus(request.status()));
        }

        return MilestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @Transactional
    public void delete(UUID milestoneId, UUID userId) {
        Milestone milestone = findMilestone(milestoneId);
        projectAuthorizationService.requireProjectManager(milestone.getProjectId(), userId);
        milestoneRepository.delete(milestone);
    }

    private Milestone findMilestone(UUID milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
    }

    private void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Due date must be on or after start date",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private MilestoneStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return MilestoneStatus.PLANNED;
        }
        try {
            return MilestoneStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid milestone status", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
