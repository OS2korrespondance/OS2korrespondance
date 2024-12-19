using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Quartz;
using S3FileUploader.Service;
using Topshelf;
using Topshelf.MicrosoftDependencyInjection;
using Topshelf.Quartz;
using Serilog;
using S3FileUploader.Model;
using System.Collections.Generic;

namespace S3FileUploader
{
    class Program
    {
        static void Main(string[] args)
        {
            IConfigurationRoot configuration = new ConfigurationBuilder()
                .AddJsonFile("appsettings.json")
                .Build();

            Log.Logger = new LoggerConfiguration().WriteTo.File(configuration["LogFile"]).CreateLogger();

            IServiceCollection services = new ServiceCollection();
            services.AddSingleton<IConfiguration>(configuration);
            services.AddSingleton<FileSyncService>();
            services.AddSingleton<FileUploadJob>();
            services.AddSingleton<FileDownloadJob>();
            services.AddSingleton<FileShareService>();
            services.AddLogging(logging => {
                logging.ClearProviders();
                logging.AddSerilog();
                logging.AddDebug();
                logging.SetMinimumLevel(LogLevel.Debug);
            });
            var serviceProvider = services.BuildServiceProvider();

            HostFactory.Run(h =>
            {
                h.UseMicrosoftDependencyInjection(serviceProvider);
                h.UsingQuartzJobFactory(() => new MicrosoftDependencyInjectionJobFactory(serviceProvider));
                h.Service<FileSyncService>(sc =>
                {
                    sc.ConstructUsingMicrosoftDependencyInjection();
                    sc.WhenStarted((s, hostControl) => s.Start(hostControl));
                    sc.WhenStopped((s, hostControl) => s.Stop(hostControl));

                    sc.ScheduleQuartzJob(q => q.WithJob(() => JobBuilder.Create<FileUploadJob>().Build())
                        .AddTrigger(() => TriggerBuilder.Create()
                        .WithCronSchedule(configuration["CronSchedule"])
                        .Build())
                        .AddTrigger(() => TriggerBuilder.Create().StartNow().Build()));

                    sc.ScheduleQuartzJob(q => q.WithJob(() => JobBuilder.Create<FileDownloadJob>().Build())
                        .AddTrigger(() => TriggerBuilder.Create()
                        .WithCronSchedule(configuration["CronSchedule"])
                        .Build())
                        .AddTrigger(() => TriggerBuilder.Create().StartNow().Build()));
                });
                h.SetDisplayName("MedcomFileSync");
                h.SetServiceName("MedcomFileSync");
                h.SetDescription("Transports messages between medcom webmail system and KMD VANS");
            });
        }
    }
}
