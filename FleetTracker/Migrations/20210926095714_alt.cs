using Microsoft.EntityFrameworkCore.Migrations;

namespace FleetTracker.Migrations
{
    public partial class alt : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<float>(
                name: "Altitude",
                table: "TruckerLog",
                type: "float",
                nullable: false,
                defaultValue: 0f);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Altitude",
                table: "TruckerLog");
        }
    }
}
