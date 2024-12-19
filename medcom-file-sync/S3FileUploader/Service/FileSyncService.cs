using Microsoft.Extensions.Logging;
using Topshelf;

namespace S3FileUploader.Service
{
    public class FileSyncService : ServiceControl
    {
        private readonly ILogger<FileSyncService> logger;
        public FileSyncService(ILogger<FileSyncService> logger)
        {
            this.logger = logger;
        }
        public bool Start(HostControl hostControl)
        {
            logger.LogInformation("Service started");
            return true;
        }

        public bool Stop(HostControl hostControl)
        {
            logger.LogInformation("Service stopped");
            return true;
        }
    }
}
