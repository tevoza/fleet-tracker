using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using FleetTracker.Models;
using FleetTracker.ViewModels;
using FleetTracker.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace FleetTracker.Controllers
{
    [Authorize]
    public class FleetController : Controller
    {
        private const long DAY_SECONDS = 60*60*24;
        private const long DAY_DISPLAY = 7;
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
        public async Task<IActionResult> ViewTrucker(string id, string upTillDate, string daysBefore)
        {
            var UpperDate = upTillDate == null ? 
                ((DateTimeOffset)DateTime.Today.AddDays(1)).ToUnixTimeSeconds() 
                : ((DateTimeOffset)DateTime.Parse(upTillDate).AddDays(1)).ToUnixTimeSeconds();
            var DaysBefore = daysBefore == null ? DAY_DISPLAY : Int64.Parse(daysBefore);
            var LowerDate = UpperDate - (DaysBefore*DAY_SECONDS);

            var manager = await _userManager.GetUserAsync(User);
            var viewTruckerViewModel = new ViewTruckerViewModel(
                _db.Trucker.Find(int.Parse(id)),
                 _db.TruckerLog.FromSqlRaw(
                    $"SELECT * FROM TruckerLog WHERE TruckerID = {id} AND TimeStamp > {LowerDate} AND TimeStamp < {UpperDate} ORDER BY TimeStamp ASC"),
                new List<Segment>(), manager, DaysBefore, UpperDate
                );
            viewTruckerViewModel.AggregatedLogs = viewTruckerViewModel.AggregateNearbyLogs();
            viewTruckerViewModel.StopLogs = viewTruckerViewModel.FindStopPoints();
            return View(viewTruckerViewModel);
        }

        // GET: /Fleet/SetRules/
        public async Task<IActionResult> SetRules(Trucker t)
        {
            var manager = await _userManager.GetUserAsync(User);
            return View(manager);
        }

        // POST: /Fleet/SetRules/
        [HttpPost, ValidateAntiForgeryToken]
        public async Task<IActionResult> SetRules(Manager m)
        {
            if (ModelState.IsValid) {
                var manager = await _userManager.GetUserAsync(User);
                manager.MaxSpeed = m.MaxSpeed;
                manager.MinSpeed = m.MinSpeed;
                _db.Manager.Update(manager);
                _db.SaveChanges();
            }
            return await Task.Run( () => RedirectToAction("Index"));
        }
        
        // GET: /Fleet/ResetTrucker/
        public async Task<IActionResult> ResetTrucker(string id)
        {
            var trucker = _db.Trucker.Single(t => t.ID == int.Parse(id));
            trucker.Verified = false;
            _db.Trucker.Update(trucker);
            _db.SaveChanges();

            return await Task.Run( () => RedirectToAction("Index"));
        }

        // GET: /Fleet/ViewTrip/
        public IActionResult ViewTrip(Segment s)
        {
            var logs =  _db.TruckerLog.FromSqlRaw(
                $"SELECT * FROM TruckerLog WHERE TruckerID = {s.TruckerID} AND TimeStamp > {s.StartTime} AND TimeStamp < {s.StopTime} ORDER BY TimeStamp ASC")
                .ToList();
            return View(logs);
        }
        
    }
}