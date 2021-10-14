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
        private const Double THRESHHOLD_METERS = 150;
        private const int STOP_TIME_MINUTES = 5;
        private const Double DEGREES_TO_METERS = 0.00001;
        private const Double THRESHHOLD = THRESHHOLD_METERS*DEGREES_TO_METERS;
        private const int STOP_TIME_SECONDS = STOP_TIME_MINUTES * 60;
        public Trucker Trucker { get; set; }
        public Manager Manager {get; set; }
        public long UpperTime {get; set; }
        public long DaysDisplay {get; set; }
        [JsonPropertyName("data")]
        public List<Segment> Segments {get; set; }
        public IQueryable<TruckerLog> TruckerLogs {get; set; }
        public List<TruckerLog> AggregatedLogs {get; set; }
        public List<TruckerLog> StopLogs {get; set; }

        public ViewTruckerViewModel(Trucker trucker, 
            IQueryable<TruckerLog> truckerLogs,
            List<Segment>  travelSegments,
            Manager manager,
            long daysDisplay,
            long upperTime)
        {
            Trucker = trucker;
            TruckerLogs = truckerLogs;
            Segments = travelSegments;
            Manager = manager;
            DaysDisplay = daysDisplay;
            UpperTime = upperTime;
        }

        private Double GetDelta(float latA, float lonA, float latB, float lonB)
        {
            return Math.Sqrt(Math.Pow((latA - latB),2) + Math.Pow((lonA - lonB), 2));
        }
        public List<TruckerLog> AggregateNearbyLogs()
        {
            List<TruckerLog> aggregatedLogs = new List<TruckerLog>();
            if (TruckerLogs.Count() == 0) {
                return aggregatedLogs;
            }
            var lst = TruckerLogs.ToList();

            aggregatedLogs.Add(lst[0]);
            var last = aggregatedLogs[0];

            for(var l = 0; l < lst.Count - 1; l++)
            {
                if ((GetDelta(lst[l].Latitude, lst[l].Longitude, last.Latitude,last.Longitude) > THRESHHOLD)
                && (GetDelta(lst[l+1].Latitude, lst[l+1].Longitude, last.Latitude,last.Longitude) > THRESHHOLD))
                {
                    aggregatedLogs.Add(lst[l]);
                    last = lst[l];
                }
            }
            return aggregatedLogs;
        }

        public List<TruckerLog> FindStopPoints()
        {
            List<TruckerLog> stopLogs = new List<TruckerLog>();
            if (AggregatedLogs.Count == 0) {
                return stopLogs;
            }

            int startLogCount = 0;
            int startPinCount = 0;
            for (var i = 0; i < AggregatedLogs.Count-1; i++)
            {
                if ((AggregatedLogs[i+1].TimeStamp - AggregatedLogs[i].TimeStamp) > STOP_TIME_SECONDS)
                {
                    stopLogs.Add(AggregatedLogs[i]);
                    if (i == 0)
                    {
                        startLogCount++;
                    } else {
                        AddSegment(AggregatedLogs[startPinCount], AggregatedLogs[startLogCount], AggregatedLogs[i]);
                        startPinCount = i;
                        startLogCount = i+1;
                    }
                }
            }
            AddSegment(AggregatedLogs[startPinCount], AggregatedLogs[startLogCount], AggregatedLogs[AggregatedLogs.Count-1]);
            return stopLogs;
        }

        void AddSegment(TruckerLog pinStart, TruckerLog logStart, TruckerLog logStop)
        {
            var segLogs = TruckerLogs.Where(x => x.TimeStamp >= logStart.TimeStamp && x.TimeStamp <= logStop.TimeStamp).ToList();
            var segment = new Segment
            {
                PinStart = pinStart,
                LogStart = logStart,
                LogStop = logStop,

                StartTime = logStart.TimeStamp,
                StopTime = logStop.TimeStamp,
                TravelTime = logStop.TimeStamp - logStart.TimeStamp,

                Points = segLogs.Count(),
                MaxSpeed = segLogs.Max(m => m.Speed),
                AvgSpeed = segLogs.Average(m => m.Speed),
                MaxSpeedBreaks = segLogs.Where(x => x.Speed > Manager.MaxSpeed).Count(),
                Delay = Segments.Count() > 0 ? (logStart.TimeStamp - pinStart.TimeStamp) : null,
                TruckerID = Trucker.ID
            };
            Segments.Add(segment);
        }
    }

    public class Segment
    {
        public long? Delay {get; set; }
        //starting pin, but info won't start logging from here
        public TruckerLog PinStart {get; set; } 
        public TruckerLog LogStart {get; set; }
        public TruckerLog LogStop {get; set; }
        public long StartTime {get; set; }
        public long StopTime {get; set; }
        public long TravelTime {get; set; }
        public int Points {get; set; }
        public float MaxSpeed {get; set; }
        public float AvgSpeed {get; set; }
        public int MaxSpeedBreaks {get; set; }
        public int TruckerID {get; set; }
    }
}