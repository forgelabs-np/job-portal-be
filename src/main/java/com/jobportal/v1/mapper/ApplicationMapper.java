package com.jobportal.v1.mapper;

import com.jobportal.v1.dto.admin.response.AdminApplicationResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApplicationMapper {

    // Agency applications
    long countAgencyApplications(@Param("jobDemandId") Long jobDemandId,
                                 @Param("agencyId") Long agencyId,
                                 @Param("status") String status);

    List<AdminApplicationResponse> getAgencyApplications(@Param("jobDemandId") Long jobDemandId,
                                                         @Param("agencyId") Long agencyId,
                                                         @Param("status") String status,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset);

    // Self-candidate applications
    long countSelfApplications(@Param("jobDemandId") Long jobDemandId,
                               @Param("status") String status);

    List<JobApplicationResponse> getSelfApplications(@Param("jobDemandId") Long jobDemandId,
                                                     @Param("status") String status,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);
}