package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@EnableCaching
@EnableScheduling
public class S3Service {
	private final S3Client s3Client;
	private final SecretKeySpec secretKeySpec;
	private final MedcomMailboxConfiguration config;

    public S3Service(S3Client s3Client, MedcomMailboxConfiguration config, @Nullable SecretKeySpec secretKeySpec) {
        this.s3Client = s3Client;
        this.secretKeySpec = secretKeySpec;
        this.config = config;
    }

    public List<String> getFileKeysFromFolder(String foldername) {
		String bucket = config.getS3().getBucketName();
		ArrayList<String> result = new ArrayList<>();
		final ListObjectsV2Response objects = s3Client.listObjectsV2(ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix(foldername + "/")
				.delimiter("/")
				.build());

		for (S3Object summary : objects.contents()) {
			if (!summary.key().endsWith("/")) {
				result.add(summary.key());
			}
		}
		
		return result;
	}

	public String upload(String folder, String filename, byte[] file) {
		String key = folder + "/" + filename + (secretKeySpec != null ? ".encrypted" : "");

		s3Client.putObject(PutObjectRequest.builder()
						.bucket(config.getS3().getBucketName())
						.key(key)
						.build(),
				createBody(file));
		return key;
	}
	
	@SneakyThrows
    public byte[] downloadBytes(String key) throws IOException {
		final String bucket = config.getS3().getBucketName();
		try {
			s3Client.headObject(HeadObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build());
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.debug("Not found. Bucket: " + bucket + " Key: " + key);
			}
			return null;
		}
		final ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build());
		byte[] readBytes = response.readAllBytes();
		if (key.endsWith(".encrypted") && secretKeySpec != null) {
			return decrypt(readBytes);
		} else if (key.endsWith(".encrypted")) {
			throw new RuntimeException("Cannot decrypt message encryption key missing, key=" + key);
		}
		return readBytes;
    }

	private byte[] decrypt(byte[] readBytes) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
		try (CipherInputStream inputStream = new CipherInputStream(new ByteArrayInputStream(readBytes), cipher)) {
			return StreamUtils.copyToByteArray(inputStream);
		}
	}

	private RequestBody createBody(byte[] bytes) {
		if (secretKeySpec != null) {
			try {
				Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
				GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
				cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
				long length = cipher.getOutputSize(bytes.length);
				return RequestBody.fromInputStream(new CipherInputStream(new ByteArrayInputStream(bytes), cipher), length);
			} catch (final Exception ex) {
				throw new RuntimeException("Encryption failed - likely encryption error", ex);
			}
		}
		return RequestBody.fromBytes(bytes);
	}

	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(config.getS3().getBucketName())
				.key(key)
				.build());
	}

}
