using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using RestSharp;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;

namespace S3FileUploader.Service
{
    public class FileShareService
    {
        private readonly ILogger<FileShareService> _logger;
        private string restUrl;
        private string apiKey;

        public FileShareService(ILogger<FileShareService> logger, IConfiguration configuration)
        {
            _logger = logger;

            restUrl = configuration["FileShare:url"];

            // Check if url ends with "/" if yes then remove it
            if ('/'.Equals(restUrl[restUrl.Length - 1]))
            {
                restUrl = restUrl.Substring(0, restUrl.Length - 1);
            }

            apiKey = configuration["FileShare:apiKey"];
        }

        public void UploadFile(string fileName, Stream fileStream)
        {
            try
            {
                byte[] payload = null;
                using (var memoryStream = new MemoryStream())
                {
                    fileStream.CopyTo(memoryStream);
                    payload = memoryStream.ToArray();
                }

                var client = new HttpClient();
                HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Post, $"{restUrl}/files?name={fileName}");
                request.Headers.Add("ApiKey", apiKey);
                request.Content = new ByteArrayContent(payload);

                var response = client.SendAsync(request).Result;
                if (response.IsSuccessStatusCode)
                {
                    _logger.LogInformation("Upload file " + fileName + " with HTTP status: " + response.StatusCode);
                }
                else
                {
                    _logger.LogWarning("Upload failed for " + fileName + " with HTTP status: " + response.StatusCode);
                }
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Failed to upload file {0}", fileName);
            }
        }

        public void DeleteFile(string fileName)
        {
            try
            {
                var client = new RestClient($"{restUrl}/files");
                var request = new RestRequest(Method.DELETE);
                request.AddHeader("ApiKey", apiKey);
                request.AddParameter("file", fileName, ParameterType.QueryString);

                var response = client.Execute(request);
                _logger.LogInformation("Deleted file " + fileName + " with HTTPStatus: " + response.StatusCode);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to delete file : " + fileName);
                throw;
            }
        }

        public void DownloadFile(string fileName, string saveTo)
        {
            try
            {
                using (FileStream fs = new FileStream(saveTo, FileMode.OpenOrCreate, FileAccess.Write))
                {
                    fs.SetLength(0);
                    var client = new RestClient($"{restUrl}/files");
                    var request = new RestRequest(Method.GET);
                    request.AddHeader("ApiKey", apiKey);
                    request.AddParameter("file", fileName, ParameterType.QueryString);

                    // avoid memory buffering of request content
                    request.ResponseWriter = responseStream =>
                    {
                        using (responseStream)
                        {
                            responseStream.CopyTo(fs);
                        }
                    };
                    var response = client.Execute(request);

                    _logger.LogInformation("Download file " + fileName + " with HTTPStatus: " + response.StatusCode);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download file : " + fileName);
                throw;
            }
        }

        public List<string> GetFiles()
        {
            try
            {
                var client = new RestClient($"{restUrl}/files/list");
                var request = new RestRequest(Method.GET);
                request.AddHeader("ApiKey", apiKey);
                var response = client.Execute<List<string>>(request);

                return response.Data;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to get files");
                throw;
            }
        }
    }
}
