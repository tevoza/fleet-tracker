namespace FleetTracker.Models
{
    public class TruckerLog
    {
        public uint ID {get; set; }
        public virtual Trucker Trucker {get; set; }
        public uint TimeStamp {get; set; }
        public float Latitude {get; set; }
        public float Longitude {get; set; }
        public byte Speed {get; set; }
        public float Acceleration {get; set; }
    }
}