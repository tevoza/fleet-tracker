using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using FleetTracker.Models;
using FleetTracker.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace FleetTracker.Controllers
{
    [Authorize]
    public class FleetController : Controller
    {
        private readonly ILogger<FleetController> _logger;
        private readonly UserManager<Manager> _userManager;
        private readonly ApplicationDbContext _db;

        public FleetController(ILogger<FleetController> logger, UserManager<Manager> userManager, ApplicationDbContext dbcontext)
        {
            _db = dbcontext;
            _logger = logger;
            _userManager = userManager;
        }
        // GET: /Fleet/Index/
        public async Task<IActionResult> Index()
        {
            var manId = _userManager.GetUserId(User);
            var truckers = _db.Trucker.FromSqlRaw($"SELECT * FROM Trucker WHERE ManagerId = '{manId}'");
            return await Task.Run( () => View(truckers));
        }

        // GET: /Fleet/AddTrucker/
        public IActionResult AddTrucker()
        {
            return View();
        }

        // POST: /Fleet/AddTrucker/
        [HttpPost, ValidateAntiForgeryToken]
        public async Task<IActionResult> AddTrucker(Trucker t)
        {
            if (ModelState.IsValid) {
                t.Manager = await _userManager.GetUserAsync(User);
                _db.Trucker.Add(t);
                _db.SaveChanges();
            }
            return await Task.Run( () => RedirectToAction("Index"));
        }
        // GET: /Fleet/ViewTrucker/
        public IActionResult ViewTrucker(string id)
        {
            var truckers = _db.TruckerLog.FromSqlRaw($"SELECT * FROM TruckerLog WHERE TruckerID = '{id}'");
            return View(truckers);
        }
    }
}
