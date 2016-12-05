package thingtranslator2.jalle.com.thingtranslator2.Rest;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiInterface {
    //      String ENDPOINT = "http://192.168.0.11:1337/";
    //  String ENDPOINT = "https://rest-service1.herokuapp.com/";http://thingtranslatorapi2.apphb.com/api/upload
    String ENDPOINT = "http://thingtranslatorapi2.apphb.com/api/";

    @Multipart
    @POST("upload")
    Call<Translation> upload(@Part("image\"; filename=\"pic.jpg\" ") RequestBody file, @Part("FirstName") RequestBody langCode1);

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(ENDPOINT)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
}


