package com.example.bugtracker.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IssueApi {

    @GET("issues")
    suspend fun getIssues(): Response<List<IssueDto>>

    @POST("issues")
    suspend fun createIssue(@Body issue: IssueDto): Response<IssueDto>

    @PUT("issues/{id}")
    suspend fun updateIssue(
        @Path("id") id: String,
        @Body issue: IssueDto
    ): Response<IssueDto>

    @DELETE("issues/{id}")
    suspend fun deleteIssue(@Path("id") id: String): Response<Unit>
}
