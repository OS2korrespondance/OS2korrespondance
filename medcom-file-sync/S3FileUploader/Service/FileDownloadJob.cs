using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Quartz;
using S3FileUploader.Model;
using System;
using System.Collections.Generic;
using System.IO;

namespace S3FileUploader.Service
{
    [DisallowConcurrentExecution]
    public class FileDownloadJob : IJob
    {
        private readonly ILogger _logger;
        private readonly FileShareService _fileShareService;
        private readonly List<FileDownload> _fileDownloads = new List<FileDownload>();

        public FileDownloadJob(ILogger<FileDownloadJob> logger, IConfiguration configuration, FileShareService fileShareService)
        {
            _logger = logger;
            configuration.GetSection("FileDownloads").Bind(_fileDownloads);
            _fileShareService = fileShareService;
        }

        public void Execute(IJobExecutionContext context)
        {
            try
            {
                var bucketFiles = _fileShareService.GetFiles();
                foreach (var downloads in _fileDownloads)
                {
                    var filesForDownload = GetFiles(downloads, bucketFiles);
                    if (filesForDownload.Count == 0)
                    {
                        _logger.LogInformation("No files found for download");
                    }

                    foreach (string file in filesForDownload)
                    {
                        var downloadFileName = Path.GetFileName(file);

                        _logger.LogInformation("Downloading file {0} as {1}", file, downloadFileName);

                        _fileShareService.DownloadFile(file, Path.Combine(downloads.DirectoryPath, downloadFileName));

                        _fileShareService.DeleteFile(file);
                    }                    
                }
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Failed to execute FileDownload job");
            }
        }

        private List<string> GetFiles(FileDownload fileDownload, List<string> files)
        {
            List<string> result = new List<string>();

            foreach (string file in files)
            {
                if (file.StartsWith(fileDownload.S3Path) && !file.EndsWith("/"))
                {
                    result.Add(file);
                }
            }

            return result;
        }
    }
}
