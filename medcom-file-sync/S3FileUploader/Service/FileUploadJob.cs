using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Quartz;
using S3FileUploader.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace S3FileUploader.Service
{
    [DisallowConcurrentExecution]
    public class FileUploadJob : IJob
    {
        private readonly ILogger<FileUploadJob> logger;
        private readonly FileUpload fileUpload = new FileUpload();
        private readonly List<FileDownload> fileDownloads = new List<FileDownload>();
        private readonly FileShareService fileShareService;
        private static List<string> uploadedFiles = new List<string>();
        private static readonly DateTime ServiceStartTime = DateTime.UtcNow;
        private readonly MonitoringClient monitoringClient;

        public FileUploadJob(ILogger<FileUploadJob> logger, IConfiguration configuration, FileShareService fileShareService, MonitoringClient monitoringClient)
        {
            this.logger = logger;
            configuration.GetSection("FileUpload").Bind(fileUpload);
            configuration.GetSection("FileDownloads").Bind(fileDownloads);
            this.fileShareService = fileShareService;
            this.monitoringClient = monitoringClient;
        }

        public Task Execute(IJobExecutionContext context)
        {
            // Send IsAlive
            monitoringClient.SendIsAlive(
                (long)(DateTime.UtcNow - ServiceStartTime).TotalSeconds,
                GetPathStatuses());

            try
            {
                var newestFiles = GetFiles(fileUpload);
                if (newestFiles.Count == 0)
                {
                    logger.LogInformation("Found no new files in folder {0}", fileUpload.DirectoryPath);
                    return Task.CompletedTask;
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

                    using (var fileStream = File.OpenRead(file))
                    {
                        fileShareService.UploadFile(uploadFileName, fileStream);
                    }

                    string to = Path.Combine(fileUpload.ProcessedPath, fileName);
                    logger.LogInformation("Moving processed file from " + file + " to " + to);
                    File.Move(file, to);

                    uploadedFiles.Remove(file);
                }
            }
            catch (Exception e)
            {
                logger.LogError(e, "Failed to execute FileUpload job for {0}", fileUpload.Name);
                monitoringClient.ReportError("FileUploadJob", e.Message);
            }
            return Task.CompletedTask;
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

        private List<object> GetPathStatuses()
        {
            var paths = new List<object>();

            // FileUpload path
            paths.Add(new
            {
                name = fileUpload.Name + "_DirectoryPath",
                directoryPath = fileUpload.DirectoryPath,
                accessible = TestDirectoryAccess(fileUpload.DirectoryPath)
            });
            paths.Add(new
            {
                name = fileUpload.Name + "_ProcessedPath",
                directoryPath = fileUpload.ProcessedPath,
                accessible = TestDirectoryAccess(fileUpload.ProcessedPath)
            });

            // FileDownload paths
            foreach (var download in fileDownloads)
            {
                paths.Add(new
                {
                    name = download.Name,
                    directoryPath = download.DirectoryPath,
                    accessible = TestDirectoryAccess(download.DirectoryPath)
                });
            }

            return paths;
        }

        private bool TestDirectoryAccess(string path)
        {
            try
            {
                if (!Directory.Exists(path))
                {
                    return false;
                }

                // Test read access by listing files
                Directory.EnumerateFiles(path);
                return true;
            }
            catch (Exception)
            {
                return false;
            }
        }
    }
}
