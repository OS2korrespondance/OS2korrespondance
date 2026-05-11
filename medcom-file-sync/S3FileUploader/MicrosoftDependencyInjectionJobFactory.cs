using Quartz;
using Quartz.Spi;
using System;

namespace S3FileUploader
{
    internal class MicrosoftDependencyInjectionJobFactory : IJobFactory
    {
        private readonly IServiceProvider serviceProvider;

        public MicrosoftDependencyInjectionJobFactory(IServiceProvider serviceProvider)
        {
            this.serviceProvider = serviceProvider;
        }

        public IJob NewJob(TriggerFiredBundle bundle, IScheduler scheduler)
        {
            return (IJob)serviceProvider.GetService(bundle.JobDetail.JobType);
        }

        public void ReturnJob(IJob job)
        {
            (job as IDisposable)?.Dispose();
        }
    }
}
