using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using S3FileUploader.Service;
using Serilog;
using Topshelf;
using Topshelf.MicrosoftDependencyInjection;

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
            services.AddSingleton<MonitoringClient>();
            services.AddLogging(logging =>
            {
                logging.ClearProviders();
                logging.AddSerilog();
                logging.AddDebug();
                logging.SetMinimumLevel(LogLevel.Debug);
            });
            var serviceProvider = services.BuildServiceProvider();

            HostFactory.Run(h =>
            {
                h.UseMicrosoftDependencyInjection(serviceProvider);
                h.Service<FileSyncService>(sc =>
                {
                    sc.ConstructUsingMicrosoftDependencyInjection();
                    sc.WhenStarted((s, hostControl) => s.Start(hostControl));
                    sc.WhenStopped((s, hostControl) => s.Stop(hostControl));
                });
                h.SetDisplayName("MedcomFileSync");
                h.SetServiceName("MedcomFileSync");
                h.SetDescription("Transports messages between medcom webmail system and KMD VANS");
            });
        }
    }
}
