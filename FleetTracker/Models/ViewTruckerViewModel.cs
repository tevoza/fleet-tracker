using FleetTracker.Models;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;
using System.Linq;
using System;

namespace FleetTracker.ViewModels
{
    public class ViewTruckerViewModel
    {
        public Trucker Trucker { get; set; }
        public IQueryable<TruckerLog> TruckerLogs {get; set; }
        public List<TruckerLog> AggregatedLogs {get; set; }
        public List<TruckerLog> StopLogs {get; set; }
        private Double distanceThresh = 0.00001*75;
        public ViewTruckerViewModel(Trucker trucker, 
            IQueryable<TruckerLog> truckerLogs)
        {
            Trucker = trucker;
            TruckerLogs = truckerLogs;
        }

        private Double GetDelta(float latA, float lonA, float latB, float lonB)
        {
            return Math.Sqrt(Math.Pow((latA - latB),2) + Math.Pow((lonA - lonB), 2));
        }
        public List<TruckerLog> AggregateNearbyLogs()
        {
            List<TruckerLog> aggregatedLogs = new List<TruckerLog>();
            aggregatedLogs.Add(TruckerLogs.First());
            var last = aggregatedLogs[0];
            foreach (var log in TruckerLogs)
            {
                if (GetDelta(
                    log.Latitude,
                    log.Longitude,
                    last.Latitude,
                    last.Longitude
                ) > distanceThresh)
                {
                    aggregatedLogs.Add(log);
                    last = aggregatedLogs[aggregatedLogs.Count - 1];
                }
            }
            return aggregatedLogs;
        }

        public List<TruckerLog> FindStopPoints()
        {
            List<TruckerLog> stopLogs = new List<TruckerLog>();
            stopLogs.Add(AggregatedLogs.First());
            for (var i = 1; i < AggregatedLogs.Count - 1; i++)
            {
                if ((AggregatedLogs[i+1].TimeStamp - AggregatedLogs[i].TimeStamp) > 5*60)
                {
                    stopLogs.Add(AggregatedLogs[i]);
                }
            }
            stopLogs.Add(AggregatedLogs.Last());
            return stopLogs;
        }

    }
}