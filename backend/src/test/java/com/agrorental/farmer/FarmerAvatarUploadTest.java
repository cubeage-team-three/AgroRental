package com.agrorental.farmer;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farmer Profile Avatar File Upload Unit Tests")
class FarmerAvatarUploadTest {

    @Mock
    private FarmerRepository farmerRepository;

    @InjectMocks
    private FarmerProfileService farmerProfileService;

    private Farmer sampleFarmer;

    @BeforeEach
    void setUp() {
        sampleFarmer = Farmer.builder()
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        sampleFarmer.setId(1L);
    }

    @Test
    @DisplayName("Valid avatar image upload saves file and updates farmer profile image URL")
    void testUploadValidAvatarImage() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake image content bytes".getBytes()
        );

        when(farmerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmer));
        when(farmerRepository.save(any(Farmer.class))).thenAnswer(i -> i.getArgument(0));

        FarmerProfileResponse response = farmerProfileService.uploadAvatar(1L, validFile);

        assertNotNull(response);
        assertNotNull(response.getProfileImage());
        assertTrue(response.getProfileImage().startsWith("/uploads/avatars/"));
        assertTrue(response.getProfileImage().endsWith(".png"));
        verify(farmerRepository, times(1)).save(sampleFarmer);
    }

    @Test
    @DisplayName("Invalid file extension (e.g. .exe) is rejected with BadRequestException")
    void testRejectDangerousFileType() {
        MockMultipartFile dangerousFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "fake exe content".getBytes()
        );

        when(farmerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmer));

        assertThrows(BadRequestException.class, () -> farmerProfileService.uploadAvatar(1L, dangerousFile));
        verify(farmerRepository, never()).save(any(Farmer.class));
    }

    @Test
    @DisplayName("File exceeding 5MB size limit is rejected with BadRequestException")
    void testRejectOversizedFile() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "big.jpeg",
                "image/jpeg",
                largeBytes
        );

        when(farmerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmer));

        assertThrows(BadRequestException.class, () -> farmerProfileService.uploadAvatar(1L, largeFile));
        verify(farmerRepository, never()).save(any(Farmer.class));
    }
}
