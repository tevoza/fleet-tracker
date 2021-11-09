using System.Collections.Generic;
using Microsoft.AspNetCore.Identity;
using System.ComponentModel.DataAnnotations;

namespace FleetTracker.Models
{
    public class Manager : IdentityUser
    {
        public string FleetName {get; set; }
        public ICollection<Trucker> Truckers {get; set; }

        [Display(Name="Maximum speed")]
        public int MaxSpeed {get; set;} = 100;
        [Display(Name="Minimum speed")]
        public int MinSpeed {get; set;} = 30;
        [Display(Name="Maximum Acceleration")]
        public float MaxAccel {get; set;} = 2.5f;
    }
}