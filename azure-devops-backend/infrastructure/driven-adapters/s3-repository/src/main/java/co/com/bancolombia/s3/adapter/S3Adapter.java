package co.com.bancolombia.s3.adapter;

import co.com.bancolombia.s3.operations.S3Operations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class S3Adapter /* implements SomeGatewayFromDomain*/ {

    private final S3Operations s3Operations;
}
