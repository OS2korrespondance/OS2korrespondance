using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Quartz;
using RestSharp;
using S3FileUploader.Model;
using System;
using System.Collections.Generic;
using System.IO;

namespace S3FileUploader.Service
{
    [DisallowConcurrentExecution]
    public class FileUploadJob : IJob
    {
        private readonly ILogger<FileUploadJob> logger;
        private readonly FileUpload fileUpload = new FileUpload();
        private readonly FileShareService fileShareService;
        private static List<string> uploadedFiles = new List<string>();

        public FileUploadJob(ILogger<FileUploadJob> logger, IConfiguration configuration, FileShareService fileShareService)
        {
            this.logger = logger;
            configuration.GetSection("FileUpload").Bind(fileUpload);
            this.fileShareService = fileShareService;
        }

        public void Execute(IJobExecutionContext context)
        {
            // ping endpoint to inform backend the on-premise agent is still running
            try
            {
                var client = new RestClient(fileUpload.IsAliveUrl);
                var request = new RestRequest(Method.GET);
                client.Execute(request);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Failed to ping mailbox system");
            }

            try
            {
                var newestFiles = GetFiles(fileUpload);
                if (newestFiles.Count == 0)
                {
                    logger.LogInformation("Found no new files in folder {0}", fileUpload.DirectoryPath);
                    return;
                }

                foreach (string file in newestFiles)
                {
                    if (IsFileLocked(file))
                    {
                        logger.LogInformation("Postponing upload because file is locked: {0}", file);
                        continue;
                    }

                    if (uploadedFiles.Contains(file))
                    {
                        logger.LogInformation("File already uploaded: {0}", file);
                        continue;
                    }

                    var fileName = Path.GetFileName(file);
                    var uploadFileName = Path.Combine("in/", fileName);

                    logger.LogInformation("Uploading file {0} as {1}", file, uploadFileName);

                    using (var fileStream = File.OpenRead(file))
                    {
                        fileShareService.UploadFile(uploadFileName, fileStream);
                    }

                    uploadedFiles.Add(file);

                    string to = Path.Combine(fileUpload.ProcessedPath, fileName);

                    logger.LogInformation("Moving processed file from " + file + " to " + to);

                    File.Move(file, to);
                }
            }
            catch (Exception e)
            {
                logger.LogError(e, "Failed to execute FileUpload job for {0}", fileUpload.Name);
            }
        }

        private bool IsFileLocked(string filePath)
        {
            var file = new FileInfo(filePath);

            try
            {
                using (FileStream stream = file.Open(FileMode.Open, FileAccess.Read, FileShare.None))
                {
                    ;
                }
            }
            catch (IOException)
            {
                return true;
            }

            return false;
        }

        private List<string> GetFiles(FileUpload fileUpload)
        {
            var files = Directory.EnumerateFiles(fileUpload.DirectoryPath);

            var newFiles = new List<string>();
            foreach (var file in files)
            {
                newFiles.Add(file);
            }

            return newFiles;
        }
    }
}
