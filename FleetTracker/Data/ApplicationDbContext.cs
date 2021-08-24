using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Identity;
using MySql.EntityFrameworkCore.Extensions;
using FleetTracker.Models;

namespace FleetTracker.Data
{
    public class ApplicationDbContext : IdentityDbContext
    {
        public DbSet <Manager> Manager {get; set;}
        public DbSet <Trucker> Trucker {get; set;}
        public DbSet <TruckerLog> TruckerLog {get; set;}
        public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
            : base(options)
        {
        }

        protected override void OnModelCreating(ModelBuilder builder)
        {
            base.OnModelCreating(builder);
            //aspnet identity defaults
            builder.Entity<IdentityRole>(entity => entity.Property(m => m.Id).HasMaxLength(450));
            
            builder.Entity<IdentityUserLogin<string>>(entity =>
            {
                entity.Property(m => m.LoginProvider).HasMaxLength(127);
                entity.Property(m => m.ProviderKey).HasMaxLength(127);
            });
            
            builder.Entity<IdentityUserRole<string>>(entity =>
            {
                entity.Property(m => m.UserId).HasMaxLength(127);
                entity.Property(m => m.RoleId).HasMaxLength(127);
            });
            
            builder.Entity<IdentityUserToken<string>>(entity =>
            {
                entity.Property(m => m.UserId).HasMaxLength(127);
                entity.Property(m => m.LoginProvider).HasMaxLength(127);
                entity.Property(m => m.Name).HasMaxLength(127);
            });
            //custom trucker stuff
            builder.Entity<Trucker>(entity =>
            {
                entity.HasKey(e => e.ID);
                entity.HasOne(d => d.Manager).WithMany(p => p.Truckers);
                entity.Property(e => e.Name).HasMaxLength(127);
            });

            builder.Entity<TruckerLog>(entity =>
            {
                entity.HasKey(e => e.ID);
                entity.Property(e => e.TimeStamp).IsRequired();
                entity.HasOne(d => d.Trucker).WithMany(p => p.TruckerLogs);
            });
        }
    }
}

//docker run --name FleetDB -p 3306:3306 -e MYSQL_ROOT_PASSWORD=pass -d mysql
//dotnet ef migrations add CreateIdentitySchema
//dotnet ef database update