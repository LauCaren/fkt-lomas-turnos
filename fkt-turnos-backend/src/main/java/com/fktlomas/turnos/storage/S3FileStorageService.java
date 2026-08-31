package com.fktlomas.turnos.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Storage en un bucket S3-compatible (AWS S3 o Cloudflare R2, seteando {@code app.storage.s3.endpoint}).
 * Se activa con {@code app.storage.type=s3}. El bucket queda privado siempre: los archivos
 * se sirven a través de los controllers existentes (que ya aplican su propio control de acceso),
 * nunca con una URL pública de S3 — así el CV de una postulación sigue siendo admin-only.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.region:auto}")
    private String region;

    @Value("${app.storage.s3.endpoint:}")
    private String endpoint;

    @Value("${app.storage.s3.access-key}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    void inicializar() {
        var builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        // R2 no tiene el concepto de regiones de AWS; "auto" alcanza porque siempre se
        // pisa el endpoint. Para AWS S3 real, dejar app.storage.s3.endpoint vacío y
        // poner la región real (ej. sa-east-1).
        builder.region(Region.of("auto".equals(region) ? "us-east-1" : region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        this.s3Client = builder.build();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, MultipartFile archivo) throws IOException {
        String clave = carpeta + "/" + nombreArchivo;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType(archivo.getContentType())
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));
        return clave;
    }

    @Override
    public Optional<Resource> cargar(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        try {
            byte[] contenido = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(clave)
                    .build()).asByteArray();
            return Optional.of(new ByteArrayResource(contenido));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void borrar(String clave) {
        if (clave == null) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
        } catch (RuntimeException ignored) {
            // No es crítico: queda un archivo huérfano en el bucket, no afecta el funcionamiento.
        }
    }
}
