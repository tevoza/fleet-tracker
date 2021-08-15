using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace FleetTracker.Models
{
    public class Trucker
    {
        public virtual Manager Manager {get; set; }
        [Key]
        public int ID {get; set; }
        [Display(Name="Name")]
        [Required]
        public string Name {get; set; }
        [Display(Name="Vehicle Registration")]
        [Required]
        public string VehicleNumber {get; set; }
        public virtual ICollection<TruckerLog> TruckerLogs {get; set;}
    }
}