namespace S3FileUploader.Model
{
    public class FileUpload
    {
        public string Name { get; set; }
        public string DirectoryPath { get; set; }
        public string ProcessedPath { get; set; }
        public string IsAliveUrl { get; set; }
    }
}
