using System.Collections.Generic;
using Microsoft.AspNetCore.Identity;

namespace FleetTracker.Models
{
    public class Manager : IdentityUser
    {
        public string FleetName {get; set; }
        public ICollection<Trucker> Truckers {get; set; }
    }
}