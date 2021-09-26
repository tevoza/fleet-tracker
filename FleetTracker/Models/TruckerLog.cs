using System.Text.Json.Serialization;
namespace FleetTracker.Models
{
    public class TruckerLog
    {
        public int ID {get; set; }
        [JsonIgnore]
        public virtual Trucker Trucker {get; set; }
        public long TimeStamp {get; set; }
        public float Latitude {get; set; }
        public float Longitude {get; set; }
        public float Speed {get; set; }
        public float Acceleration {get; set; }
        public float Altitude {get; set; }
    }
}