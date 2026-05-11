using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using RestSharp;
using S3FileUploader.Model;
using System;
using System.Collections.Generic;

namespace S3FileUploader.Service
{
    public class MonitoringClient
    {
        private readonly ILogger<MonitoringClient> logger;
        private readonly string baseUrl;
        private readonly string apiKey;

        public MonitoringClient(ILogger<MonitoringClient> logger, IConfiguration configuration)
        {
            this.logger = logger;
            var fileUpload = new FileUpload();
            configuration.GetSection("FileUpload").Bind(fileUpload);
            this.baseUrl = fileUpload.MonitorApiUrl;
            this.apiKey = fileUpload.MonitorApiKey;
        }

        public void SendIsAlive(long uptimeSeconds, List<object> pathStatuses)
        {
            var payload = new
            {
                uptimeSeconds = uptimeSeconds,
                paths = pathStatuses
            };

            Post("/isalive", payload);
        }

        public void ReportError(string jobName, string errorMessage)
        {
            var payload = new
            {
                jobName = jobName,
                errorMessage = errorMessage
            };

            Post("/error", payload);
        }

        private void Post(string path, object payload)
        {
            try
            {
                var client = new RestClient(baseUrl + path);
                var request = new RestRequest("", Method.Post);
                request.AddHeader("ApiKey", apiKey);
                request.AddJsonBody(payload);
                var response = client.Execute(request);

                if (!response.IsSuccessful)
                {
                    logger.LogWarning("Monitoring request to {0} failed - Status: {1}, Error: {2}",
                        path, response.StatusCode, response.ErrorMessage);
                }
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Failed to send monitoring request to {0}", path);
            }
        }
    }
}