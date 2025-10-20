package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.ErrorCode;
import com.mohe.spring.entity.ContactMessage;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.ContactMessageRepository;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "고객 지원", description = "문의 및 피드백 관리 API")
public class SupportController {

    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;

    public SupportController(ContactMessageRepository contactMessageRepository, UserRepository userRepository) {
        this.contactMessageRepository = contactMessageRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/contact")
    @Operation(
        summary = "문의/피드백 전송",
        description = "사용자가 앱에 대한 문의나 피드백 메시지를 전송합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "문의 접수 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "message": "문의가 접수되었습니다."
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            )
        }
    )
    public ResponseEntity<ApiResponse<ContactResponse>> sendContactMessage(
            @Parameter(description = "문의 요청", required = true)
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {
        try {
            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            ContactMessage contactMessage = new ContactMessage();
            contactMessage.setUser(user);
            contactMessage.setMessage(request.message());
            contactMessage.setCategory(request.category() != null ? request.category() : "inquiry");
            contactMessage.setStatus("pending");

            contactMessageRepository.save(contactMessage);

            ContactResponse response = new ContactResponse("문의가 접수되었습니다.");
            return ResponseEntity.ok(ApiResponse.success(response, response.message()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "문의 접수에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    // Request DTO
    public record ContactRequest(
        @NotBlank(message = "메시지는 필수입니다")
        @Size(min = 10, max = 1000, message = "메시지는 10-1000자 사이여야 합니다")
        String message,

        String category // "bug", "feature", "inquiry", "other"
    ) {}

    // Response DTO
    public record ContactResponse(
        String message
    ) {}
}
