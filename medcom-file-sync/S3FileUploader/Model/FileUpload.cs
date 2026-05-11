namespace S3FileUploader.Model
{
    public class FileUpload
    {
        public string Name { get; set; }
        public string DirectoryPath { get; set; }
        public string ProcessedPath { get; set; }
        public string MonitorApiUrl { get; set; }
        public string MonitorApiKey {  get; set; }
    }
}
