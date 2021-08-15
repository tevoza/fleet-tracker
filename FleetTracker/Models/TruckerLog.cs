using System;
namespace FleetTracker.Models
{
    public class TruckerLog
    {
        public int ID {get; set; }
        public virtual Trucker Trucker {get; set; }

        public int TimeStamp {get; set; }
        public double Longitude {get; set; }
        public double Latitude {get; set; }
        public double Speed {get; set; }
        public double Acceleration {get; set; }
    }
}