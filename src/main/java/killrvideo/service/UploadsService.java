package killrvideo.service;

import org.springframework.stereotype.Service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.uploads.UploadsServiceGrpc.UploadsServiceImplBase;
import killrvideo.uploads.UploadsServiceOuterClass.GetStatusOfVideoRequest;
import killrvideo.uploads.UploadsServiceOuterClass.GetStatusOfVideoResponse;
import killrvideo.uploads.UploadsServiceOuterClass.GetUploadDestinationRequest;
import killrvideo.uploads.UploadsServiceOuterClass.GetUploadDestinationResponse;
import killrvideo.uploads.UploadsServiceOuterClass.MarkUploadCompleteRequest;
import killrvideo.uploads.UploadsServiceOuterClass.MarkUploadCompleteResponse;

/**
 * To be implemented. Right now we
 * only support videos import from Youtube
 */
@Service
//public class UploadsService extends AbstractUploadsService {
public class UploadsService extends UploadsServiceImplBase {
    
    @Override
    public void getUploadDestination(GetUploadDestinationRequest request, StreamObserver<GetUploadDestinationResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Uploading videos is currently not supported").asRuntimeException());
    }

    @Override
    public void markUploadComplete(MarkUploadCompleteRequest request, StreamObserver<MarkUploadCompleteResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Uploading videos is currently not supported").asRuntimeException());
    }

    @Override
    public void getStatusOfVideo(GetStatusOfVideoRequest request, StreamObserver<GetStatusOfVideoResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Uploading videos is currently not supported").asRuntimeException());
    }
}
