using FleetTracker.Models;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;
using System.Linq;
using System;
using Microsoft.Extensions.Logging;
using System.Text.Json.Serialization;

namespace FleetTracker.ViewModels
{
    public class ViewTruckerViewModel
    {
        private const Double THRESHHOLD_METERS = 75;
        private const int STOP_TIME_MINUTES = 5;
        private const Double DEGREES_TO_METERS = 0.00001;
        private const Double THRESHHOLD = THRESHHOLD_METERS*DEGREES_TO_METERS;
        private const int STOP_TIME_SECONDS = STOP_TIME_MINUTES * 60;
        public Trucker Trucker { get; set; }
        public Manager Manager {get; set; }
        [JsonPropertyName("data")]
        public List<Segment> Segments {get; set; }
        public IQueryable<TruckerLog> TruckerLogs {get; set; }
        public List<TruckerLog> AggregatedLogs {get; set; }
        public List<TruckerLog> StopLogs {get; set; }

        public ViewTruckerViewModel(Trucker trucker, 
            IQueryable<TruckerLog> truckerLogs,
            List<Segment>  travelSegments,
            Manager manager)
        {
            Trucker = trucker;
            TruckerLogs = truckerLogs;
            Segments = travelSegments;
            Manager = manager;
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
                if (GetDelta(log.Latitude, log.Longitude,last.Latitude,last.Longitude) > THRESHHOLD)
                {
                    aggregatedLogs.Add(log);
                    last = log;
                }
            }
            return aggregatedLogs;
        }

        public List<TruckerLog> FindStopPoints()
        {
            List<TruckerLog> stopLogs = new List<TruckerLog>();

            int start = 0;
            for (var i = 0; i < AggregatedLogs.Count - 1; i++)
            {
                if ((AggregatedLogs[i+1].TimeStamp - AggregatedLogs[i].TimeStamp) > STOP_TIME_SECONDS)
                {
                    stopLogs.Add(AggregatedLogs[i]);
                    if (i != 0){
                        AddSegment(AggregatedLogs[start].TimeStamp, AggregatedLogs[i].TimeStamp);
                        start = i+1;
                    } else {
                        start++;
                    }
                }
            }
            AddSegment(AggregatedLogs[start].TimeStamp, AggregatedLogs.Last().TimeStamp);
            stopLogs.Add(AggregatedLogs.Last());
            return stopLogs;
        }

        void AddSegment(long tStart, long tEnd)
        {
            var segLogs = TruckerLogs.Where(x => x.TimeStamp >= tStart && x.TimeStamp <= tEnd).ToList();
            var segment = new Segment
            {
                StartTime = tStart,
                StopTime = tEnd,
                TravelTime = tEnd - tStart,
                points = segLogs.Count(),
                MaxSpeed = segLogs.Max(m => m.Speed),
                AvgSpeed = segLogs.Average(m => m.Speed),
                MaxSpeedBreaks = segLogs.Where(x => x.Speed > Manager.MaxSpeed).Count(),
                Delay = Segments.Count() > 0 ? (tStart-Segments.Last().StopTime) : null
            };
            Segments.Add(segment);
        }
    }

    public class Segment
    {
        public long? Delay {get; set; }
        [JsonPropertyName("start")]
        public long StartTime {get; set; }
        [JsonPropertyName("stop")]
        public long StopTime {get; set; }
        public long TravelTime{get; set; }
        public int points {get; set; }
        public float AvgSpeed {get; set; }
        public float MaxSpeed {get; set; }
        public int MaxSpeedBreaks{get; set; }
    }
}