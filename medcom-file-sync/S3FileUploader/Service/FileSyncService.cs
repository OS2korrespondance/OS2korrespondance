using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Quartz;
using Quartz.Impl;
using System;
using Topshelf;

namespace S3FileUploader.Service
{
    public class FileSyncService : ServiceControl
    {
        private readonly ILogger<FileSyncService> logger;
        private readonly IServiceProvider serviceProvider;
        private readonly IConfiguration configuration;
        private IScheduler scheduler;

        public FileSyncService(ILogger<FileSyncService> logger, IServiceProvider serviceProvider, IConfiguration configuration)
        {
            this.logger = logger;
            this.serviceProvider = serviceProvider;
            this.configuration = configuration;
        }

        public bool Start(HostControl hostControl)
        {
            try
            {
                var cron = configuration["CronSchedule"];

                var factory = new StdSchedulerFactory();
                scheduler = factory.GetScheduler().GetAwaiter().GetResult();
                scheduler.JobFactory = new MicrosoftDependencyInjectionJobFactory(serviceProvider);
                scheduler.Start().GetAwaiter().GetResult();

                ScheduleJob<FileUploadJob>("upload", cron);
                ScheduleJob<FileDownloadJob>("download", cron);

                logger.LogInformation("Service started");
                return true;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Failed to start service");
                return false;
            }
        }

        public bool Stop(HostControl hostControl)
        {
            try
            {
                scheduler?.Shutdown(waitForJobsToComplete: true).GetAwaiter().GetResult();
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error while shutting down scheduler");
            }
            logger.LogInformation("Service stopped");
            return true;
        }

        private void ScheduleJob<TJob>(string name, string cron) where TJob : IJob
        {
            var job = JobBuilder.Create<TJob>()
                .WithIdentity(name + "-job")
                .Build();

            var cronTrigger = TriggerBuilder.Create()
                .WithIdentity(name + "-cron")
                .ForJob(job)
                .WithCronSchedule(cron)
                .Build();

            var startNowTrigger = TriggerBuilder.Create()
                .WithIdentity(name + "-now")
                .ForJob(job)
                .StartNow()
                .Build();

            scheduler.ScheduleJob(job, cronTrigger).GetAwaiter().GetResult();
            scheduler.ScheduleJob(startNowTrigger).GetAwaiter().GetResult();
        }
    }
}
