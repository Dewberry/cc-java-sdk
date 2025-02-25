package usace.cc.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;


public class FileDataStoreS3 implements FileDataStore {
    String bucket;
    String postFix;
    StoreType storeType;
    AmazonS3 awsS3;
    AWSConfig config;
    private static String S3ROOT = "root";
    @Override
    public Boolean Copy(FileDataStore destStore, String srcPath, String destPath) {
        byte[] data;
        try {
            data = GetObject(srcPath);
            ByteArrayInputStream bias = new ByteArrayInputStream(data);
            return destStore.Put(bias, destPath);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public InputStream Get(String path) {
        byte[] data;
        try {
            data = GetObject(path);
            ByteArrayInputStream bias = new ByteArrayInputStream(data);
            return bias;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Boolean Put(InputStream data, String path) {
        byte[] bytes;
        try {
            bytes = data.readAllBytes();
            return UploadToS3(config.aws_bucket, postFix + "/" + path, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public Boolean Delete(String path) {
        DeleteObjectRequest dor = new DeleteObjectRequest(config.aws_bucket,postFix + "/" + path);
        try{
            awsS3.deleteObject(dor);
            return true;
        }catch (AmazonServiceException e)
        {
            e.printStackTrace();
            return false;
        }catch(SdkClientException e){
            e.printStackTrace();
            return false;
        }
    }

    public FileDataStoreS3(DataStore ds){
        AWSConfig acfg = new AWSConfig();
        acfg.aws_access_key_id = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID);
        if(acfg.aws_access_key_id == null)
            System.out.println("ERROR: `" + ds.getDsProfile() + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID + "` environment variable is not set.");
        acfg.aws_secret_access_key_id = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY);
        if(acfg.aws_secret_access_key_id == null)
            System.out.println("ERROR: `" + ds.getDsProfile() + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY + "` environment variable is not set.");
        acfg.aws_region = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_DEFAULT_REGION);
        if(acfg.aws_region == null)
            System.out.println("ERROR: `" + ds.getDsProfile() + "_" + EnvironmentVariables.AWS_DEFAULT_REGION + "` environment variable is not set.");
        acfg.aws_bucket = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_S3_BUCKET);
        if(acfg.aws_bucket == null)
            System.out.println("ERROR: `" + ds.getDsProfile() + "_" + EnvironmentVariables.AWS_S3_BUCKET + "` environment variable is not set.");
        if(System.getenv(ds.getDsProfile() + "_" +"S3_MOCK") == null)
            System.out.println("WARNING: `" + ds.getDsProfile() + "_" + "S3_MOCK" + "` environment variable is not set. Defaulted to false");
        acfg.aws_mock = Boolean.parseBoolean(System.getenv(ds.getDsProfile() + "_"+ "S3_MOCK"));//convert to boolean;
        acfg.aws_endpoint = System.getenv(ds.getDsProfile() + "_"+ "S3_ENDPOINT");
        if(acfg.aws_mock == true && acfg.aws_endpoint == null)
            System.out.println("ERROR: `" + ds.getDsProfile() + "_" + "S3_ENDPOINT" + "` environment variable is not set even though using mock S3.");
        acfg.aws_disable_ssl = Boolean.parseBoolean(System.getenv(ds.getDsProfile() + "_"+ "S3_DISABLE_SSL"));//convert to bool? //Never used
        if(acfg.aws_mock == true && System.getenv(ds.getDsProfile() + "_" +"S3_FORCE_PATH_STYLE") == null)
            System.out.println("WARNING: `" + ds.getDsProfile() + "_" + "S3_FORCE_PATH_STYLE" + "` environment variable is not set even though using mock S3--defaulted to false.");
        acfg.aws_force_path_style = Boolean.parseBoolean(System.getenv(ds.getDsProfile() + "_"+ "S3_FORCE_PATH_STYLE"));//convert to bool
        config = acfg;
        //System.out.println(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_DEFAULT_REGION+"::"+config.aws_region);
        //System.out.println(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID+"::"+config.aws_access_key_id);
        //System.out.println(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY+"::"+config.aws_secret_access_key_id);
        //System.out.println(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_S3_BUCKET+"::"+config.aws_bucket);

        Regions clientRegion = Regions.valueOf(config.aws_region.toUpperCase().replace("-", "_"));
        try {
            AmazonS3 s3Client = null;
            if(config.aws_mock){
                AWSCredentials credentials = new BasicAWSCredentials(config.aws_access_key_id, config.aws_secret_access_key_id);
                ClientConfiguration clientConfiguration = new ClientConfiguration();
                clientConfiguration.setSignerOverride("AWSS3V4SignerType");
                clientConfiguration.setProtocol(Protocol.HTTP);

                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(config.aws_endpoint, clientRegion.name()))
                    .withPathStyleAccessEnabled(config.aws_force_path_style)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            }else{
                AWSCredentials credentials = new BasicAWSCredentials(config.aws_access_key_id, config.aws_secret_access_key_id);
                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            }
            awsS3 = s3Client;
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
        storeType = StoreType.S3;
        String tmpRoot = ds.getParameters().get(FileDataStoreS3.S3ROOT);
        if (tmpRoot == ""){
            //error out?
            System.out.print("Missing S3 Root Paramter. Cannot create the store.");
        }
        this.bucket = config.aws_bucket;
        tmpRoot = tmpRoot.replaceFirst("^/+", "");
        this.postFix = tmpRoot;
    }
    private byte[] GetObject(String path) throws RemoteException {
        byte[] data;
        try {
            data = DownloadBytesFromS3(path);
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
        return data;
    }
    private byte[] DownloadBytesFromS3(String key) throws Exception{
        S3Object fullObject = null;
        key = postFix + "/" + key;
        try {
            fullObject = awsS3.getObject(new GetObjectRequest(bucket, key));
            System.out.println("   `" + key + "` downloaded from bucket `" + bucket + "`");
            //System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
            return fullObject.getObjectContent().readAllBytes();
        }  catch (Exception e) {
            System.out.println("error: key `" + key + "` not found in bucket `" + bucket + "`");
            throw e;
        } finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                try {
                    fullObject.close();
                }  catch (Exception e) {
                    throw e;
                }
            }
        }
    }

    private boolean UploadToS3(String bucketName, String objectKey, byte[] fileBytes) {
        try {
            InputStream stream = new ByteArrayInputStream(fileBytes);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(fileBytes.length);
            PutObjectRequest putOb = new PutObjectRequest(bucketName, objectKey,stream, meta);
            PutObjectResult response = awsS3.putObject(putOb);
            System.out.println(response.getETag());
            return true;
        } catch (SdkBaseException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
