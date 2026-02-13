package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.ClientModel;
import com.java10x.jvaMontagens.model.ParkMediaModel;
import com.java10x.jvaMontagens.model.ParkModel;
import com.java10x.jvaMontagens.repository.ClientRepository;
import com.java10x.jvaMontagens.repository.ParkMediaRepository;
import com.java10x.jvaMontagens.repository.ParkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class ParkService {
    private static final long MAX_MEDIA_FILE_SIZE_BYTES = 60L * 1024L * 1024L;

    private final ParkRepository parkRepository;
    private final ClientRepository clientRepository;
    private final ParkMediaRepository parkMediaRepository;

    public ParkService(
            ParkRepository parkRepository,
            ClientRepository clientRepository,
            ParkMediaRepository parkMediaRepository
    ) {
        this.parkRepository = parkRepository;
        this.clientRepository = clientRepository;
        this.parkMediaRepository = parkMediaRepository;
    }

    public ParkModel createPark(ParkModel park, String clientCnpj) {
        if (park.getName() == null || park.getName().isBlank()) {
            throw new IllegalArgumentException("Park name is required.");
        }

        String normalizedCnpj = DocumentUtils.normalizeCnpj(clientCnpj);
        ClientModel client = clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));

        park.setName(park.getName().trim());
        park.setClient(client);
        return parkRepository.save(park);
    }

    public List<ParkModel> listParks(String clientCnpj) {
        if (clientCnpj != null) {
            return parkRepository.findByClientCnpj(DocumentUtils.normalizeCnpj(clientCnpj));
        }
        return parkRepository.findAll();
    }

    public ParkModel updatePark(Long parkId, String name, String city, String state, String clientCnpj) {
        ParkModel existing = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));

        if (name != null && !name.isBlank()) {
            existing.setName(name.trim());
        }
        if (city != null) {
            existing.setCity(city);
        }
        if (state != null) {
            existing.setState(state);
        }
        if (clientCnpj != null && !clientCnpj.isBlank()) {
            String normalizedCnpj = DocumentUtils.normalizeCnpj(clientCnpj);
            ClientModel client = clientRepository.findById(normalizedCnpj)
                    .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
            existing.setClient(client);
        }
        return parkRepository.save(existing);
    }

    public void deletePark(Long parkId) {
        ParkModel existing = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));
        parkRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<ParkMediaItem> listParkMedia(Long parkId) {
        ParkModel park = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));

        return parkMediaRepository.findByParkIdOrderByUploadedAtDesc(parkId).stream()
                .map(media -> toParkMediaItem(park, media))
                .toList();
    }

    @Transactional
    public List<ParkMediaItem> uploadParkMedia(Long parkId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one media file is required.");
        }

        ParkModel park = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));

        List<ParkMediaModel> mediaToSave = files.stream()
                .map(file -> buildParkMedia(park, file))
                .toList();

        return parkMediaRepository.saveAll(mediaToSave).stream()
                .map(media -> toParkMediaItem(park, media))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParkMediaFile downloadParkMedia(Long mediaId) {
        ParkMediaModel media = parkMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NoSuchElementException("Park media not found for id " + mediaId));

        if (media.getFileBytes() == null || media.getFileBytes().length == 0) {
            throw new NoSuchElementException("Park media file has no content for id " + mediaId);
        }

        String contentType = media.getContentType() == null || media.getContentType().isBlank()
                ? "application/octet-stream"
                : media.getContentType();

        String fileName = media.getFileName() == null || media.getFileName().isBlank()
                ? "park-media-" + mediaId
                : media.getFileName();

        return new ParkMediaFile(fileName, contentType, media.getFileBytes());
    }

    @Transactional
    public void deleteParkMedia(Long mediaId) {
        ParkMediaModel media = parkMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NoSuchElementException("Park media not found for id " + mediaId));
        parkMediaRepository.delete(media);
    }

    private ParkMediaModel buildParkMedia(ParkModel park, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Media file cannot be empty.");
        }

        if (file.getSize() > MAX_MEDIA_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Each media file cannot exceed 60MB.");
        }

        String contentType = normalizeMediaContentType(file.getContentType(), file.getOriginalFilename());
        if (!isAllowedMediaContentType(contentType)) {
            throw new IllegalArgumentException("Only image and video files are accepted.");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read uploaded media file.");
        }

        ParkMediaModel media = new ParkMediaModel();
        media.setPark(park);
        media.setFileName(normalizeFileName(file.getOriginalFilename(), park.getId()));
        media.setContentType(contentType);
        media.setFileSize(file.getSize());
        media.setUploadedAt(LocalDateTime.now());
        media.setFileBytes(fileBytes);
        return media;
    }

    private ParkMediaItem toParkMediaItem(ParkModel park, ParkMediaModel media) {
        return new ParkMediaItem(
                media.getId(),
                park.getId(),
                park.getName(),
                media.getFileName(),
                media.getContentType(),
                media.getFileSize(),
                media.getUploadedAt()
        );
    }

    private String normalizeFileName(String originalFileName, Long parkId) {
        String candidate = originalFileName == null ? "" : originalFileName.trim();
        if (candidate.isEmpty()) {
            return "park-" + parkId + "-media";
        }
        return candidate.replaceAll("[\\r\\n\\\\/]+", "_");
    }

    private String normalizeMediaContentType(String contentType, String fileName) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if ((normalized.isEmpty() || "application/octet-stream".equals(normalized)) && fileName != null) {
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".png")) normalized = "image/png";
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) normalized = "image/jpeg";
            if (lowerName.endsWith(".gif")) normalized = "image/gif";
            if (lowerName.endsWith(".webp")) normalized = "image/webp";
            if (lowerName.endsWith(".heic")) normalized = "image/heic";
            if (lowerName.endsWith(".mp4")) normalized = "video/mp4";
            if (lowerName.endsWith(".mov")) normalized = "video/quicktime";
            if (lowerName.endsWith(".webm")) normalized = "video/webm";
            if (lowerName.endsWith(".mkv")) normalized = "video/x-matroska";
        }
        return normalized;
    }

    private boolean isAllowedMediaContentType(String contentType) {
        return contentType != null
                && (contentType.startsWith("image/") || contentType.startsWith("video/"));
    }

    public record ParkMediaItem(
            Long id,
            Long parkId,
            String parkName,
            String fileName,
            String contentType,
            Long fileSize,
            LocalDateTime uploadedAt
    ) {}

    public record ParkMediaFile(
            String fileName,
            String contentType,
            byte[] data
    ) {}
}
